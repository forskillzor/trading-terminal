package com.aandios.tradingterminal.data.api.binance

import com.aandios.tradingterminal.data.api.binance.models.BinanceDepthUpdate
import com.aandios.tradingterminal.data.api.binance.models.DepthResponse
import com.aandios.tradingterminal.data.api.binance.models.OrderBook
import com.aandios.tradingterminal.data.api.binance.models.OrderBookLevel
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.websocket.*
import io.ktor.client.request.*
import io.ktor.websocket.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.serialization.json.Json


class BinanceDomApi(
    private val client: HttpClient,
    private val limit: Int = 20
) {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }

    suspend fun getOrderBookSnapshot(symbol: String): OrderBook {
        try {

            val response: DepthResponse = client.get("https://fapi.binance.com/fapi/v1/depth") {
                url {
                    parameters.append("symbol", symbol)
                    parameters.append("limit", limit.toString())
                }
            }.body()

            // Создаем уровни
            val bids = response.bids.map { bid ->
                OrderBookLevel(
                    price = bid[0],
                    quantity = bid[1],
                    total = "0"
                )
            }.sortedByDescending { it.price.toDouble() } // Bids: от высокой к низкой

            val asks = response.asks.map { ask ->
                OrderBookLevel(
                    price = ask[0],
                    quantity = ask[1],
                    total = "0"
                )
            }.sortedBy { it.price.toDouble() } // Asks: от низкой к высокой

            // Вычисляем тоталы
            val bidsWithTotals = calculateTotals(bids, isAsk = false)
            val asksWithTotals = calculateTotals(asks, isAsk = true)

            return OrderBook(
                symbol = symbol,
                bids = bidsWithTotals,
                asks = asksWithTotals,
                lastUpdateId = response.lastUpdateId,
                timestamp = System.currentTimeMillis()
            )
        } catch (e: Exception) {
            println("❌ DOM: Snapshot failed: ${e.message}")
            throw e
        }
    }

    fun subscribeToOrderBook(symbol: String): Flow<OrderBook> = callbackFlow {
        val streamName = "${symbol.lowercase()}@depth${limit}@100ms"
        val endpoint = "wss://fstream.binance.com/ws/$streamName"

        println("🔗 DOM WebSocket: Connecting to $endpoint")

        var localOrderBook: OrderBook? = null
        var lastUpdateId = 0L
        var isFirstUpdate = true

        try {
            client.webSocket(urlString = endpoint) {
                println("✅ DOM WebSocket: CONNECTED")

                for (frame in incoming) {
                    when (frame) {
                        is Frame.Text -> {
                            val text = frame.readText()

                            try {
                                val update = json.decodeFromString<BinanceDepthUpdate>(text)

                                // Отладка первых обновлений

                                if (isFirstUpdate) {
                                    localOrderBook = getOrderBookSnapshot(symbol)
                                    lastUpdateId = localOrderBook!!.lastUpdateId
                                    isFirstUpdate = false

                                    // Проверяем синхронизацию
                                    if (update.lastUpdateId <= lastUpdateId) {
                                        println("⚠️ DOM: Skipping old update")
                                        continue
                                    }
                                }

                                if (localOrderBook != null) {
                                    val newOrderBook = applyUpdate(localOrderBook!!, update)

                                    // Проверяем, изменился ли best bid/ask
                                    val oldBestBid = localOrderBook!!.bids.firstOrNull()?.price
                                    val newBestBid = newOrderBook.bids.firstOrNull()?.price
                                    val oldBestAsk = localOrderBook!!.asks.firstOrNull()?.price
                                    val newBestAsk = newOrderBook.asks.firstOrNull()?.price

                                    localOrderBook = newOrderBook
                                    trySend(localOrderBook!!)
                                }

                            } catch (e: Exception) {
                                println("❌ DOM parse error: ${e.message}")
                            }
                        }
                        else -> {}
                    }
                }
            }
        } catch (e: Exception) {
            println("❌ DOM WebSocket failed: ${e.message}")
            delay(1000)
            throw e
        }

        close()
    }

    private fun applyUpdate(current: OrderBook, update: BinanceDepthUpdate): OrderBook {
        // Обновляем уровни
        val newBids = updateLevels(current.bids, update.bids, isAsk = false)
        val newAsks = updateLevels(current.asks, update.asks, isAsk = true)

        // Пересчитываем тоталы
        val bidsWithTotals = calculateTotals(newBids, isAsk = false)
        val asksWithTotals = calculateTotals(newAsks, isAsk = true)

        return OrderBook(
            symbol = current.symbol,
            bids = bidsWithTotals,
            asks = asksWithTotals,
            lastUpdateId = update.lastUpdateId,
            timestamp = update.eventTime
        )
    }

    private fun updateLevels(
        currentLevels: List<OrderBookLevel>,
        updates: List<List<String>>,
        isAsk: Boolean
    ): List<OrderBookLevel> {
        // Используем TreeMap для автоматической сортировки
        val levelMap = java.util.TreeMap<String, OrderBookLevel>(
            if (isAsk)
                compareBy { it.toDouble() } // Asks: по возрастанию цены
            else
                compareByDescending { it.toDouble() } // Bids: по убыванию цены
        )

        // Добавляем текущие уровни
        currentLevels.forEach { level ->
            levelMap[level.price] = level
        }

        // Применяем обновления
        updates.forEach { update ->
            val price = update[0]
            val quantity = update[1]
            val qtyDouble = quantity.toDouble()

            if (qtyDouble == 0.0) {
                levelMap.remove(price)
            } else {
                levelMap[price] = OrderBookLevel(
                    price = price,
                    quantity = quantity,
                    total = "0"
                )
            }
        }

        // Берем только нужное количество уровней
        return levelMap.values.take(limit)
    }

    private fun calculateTotals(levels: List<OrderBookLevel>, isAsk: Boolean): List<OrderBookLevel> {
        var total = 0.0
        val sortedLevels = if (isAsk) {
            levels.sortedBy { it.price.toDouble() }
        } else {
            levels.sortedByDescending { it.price.toDouble() }
        }

        return sortedLevels.map { level ->
            val qty = level.quantity.toDouble()
            total += qty
            level.copy(total = total.toString())
        }
    }
}