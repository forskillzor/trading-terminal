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

            // Binance уже возвращает отсортированные данные!
            // Bids: от лучшей (максимальная цена) к худшей (минимальная)
            // Asks: от лучшей (минимальная цена) к худшей (максимальная)
            val bids = response.bids.map { bid ->
                OrderBookLevel(
                    price = bid[0],
                    quantity = bid[1],
                    total = "0"
                )
            } // НЕ СОРТИРУЕМ!

            val asks = response.asks.map { ask ->
                OrderBookLevel(
                    price = ask[0],
                    quantity = ask[1],
                    total = "0"
                )
            } // НЕ СОРТИРУЕМ!

            // Проверка корректности
            if (bids.isNotEmpty() && asks.isNotEmpty()) {
                val bestBid = bids.first().price.toDouble()
                val bestAsk = asks.first().price.toDouble()
                if (bestBid > bestAsk) {
                    println("⚠️ DOM: WARNING - Bid ($bestBid) > Ask ($bestAsk)!")
                }
            }

            // Вычисляем тоталы (cumulative volume)
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
            e.printStackTrace()
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
        var updateCount = 0

        try {
            client.webSocket(urlString = endpoint) {
                println("✅ DOM WebSocket: CONNECTED")

                for (frame in incoming) {
                    when (frame) {
                        is Frame.Text -> {
                            val text = frame.readText()
                            updateCount++

                            try {
                                val update = json.decodeFromString<BinanceDepthUpdate>(text)

                                if (isFirstUpdate) {
                                    // Получаем снапшот
                                    localOrderBook = getOrderBookSnapshot(symbol)
                                    lastUpdateId = localOrderBook!!.lastUpdateId

                                    println("📊 DOM: Snapshot loaded with lastUpdateId: $lastUpdateId")
                                    println("📊 DOM: First update lastUpdateId: ${update.lastUpdateId}")

                                    // По документации Binance:
                                    // Пропускаем обновления пока не получим update.lastUpdateId > snapshot.lastUpdateId
                                    // fixme this updating bug
                                    if (update.lastUpdateId <= lastUpdateId) {
                                        println("⚠️ DOM: Waiting for update > $lastUpdateId")
                                        continue
                                    }

                                    isFirstUpdate = false
                                    println("Local Orderbook $localOrderBook")
                                }

                                // Применяем обновление если оно новее последнего
                                if (localOrderBook != null && !isFirstUpdate) {
                                    if (update.lastUpdateId > lastUpdateId) {
                                        val newOrderBook = applyUpdate(localOrderBook!!, update)

                                        // Проверяем изменения цен
                                        val oldBestBid = localOrderBook!!.bids.firstOrNull()?.price
                                        val newBestBid = newOrderBook.bids.firstOrNull()?.price
                                        val oldBestAsk = localOrderBook!!.asks.firstOrNull()?.price
                                        val newBestAsk = newOrderBook.asks.firstOrNull()?.price

                                        if (oldBestBid != newBestBid || oldBestAsk != newBestAsk) {
                                            println("📊 DOM Price changed at ${update.eventTime}:")
                                            println("   Bid: $oldBestBid → $newBestBid")
                                            println("   Ask: $oldBestAsk → $newBestAsk")
                                        }

                                        localOrderBook = newOrderBook
                                        lastUpdateId = update.lastUpdateId
                                        trySend(localOrderBook!!)
                                    }
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
            e.printStackTrace()
            delay(1000)
            throw e
        }

        close()
    }

    private fun applyUpdate(current: OrderBook, update: BinanceDepthUpdate): OrderBook {
        // Обновляем уровни, сохраняя порядок
        val newBids = updateLevels(current.bids, update.bids)
        val newAsks = updateLevels(current.asks, update.asks)

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
        updates: List<List<String>>
    ): List<OrderBookLevel> {
        // Используем LinkedHashMap для сохранения порядка вставки
        val levelMap = LinkedHashMap<String, OrderBookLevel>()

        // Добавляем текущие уровни
        currentLevels.forEach { level ->
            levelMap[level.price] = level
        }

        // Применяем обновления
        updates.forEach { update ->
            val price = update[0]
            val quantity = update[1]
            val qtyDouble = quantity.toDoubleOrNull() ?: 0.0

            if (qtyDouble == 0.0) {
                levelMap.remove(price)
                println("   🗑️ Removed level: $price")
            } else {
                levelMap[price] = OrderBookLevel(
                    price = price,
                    quantity = quantity,
                    total = "0"
                )
                println("   📝 Updated level: $price @ $quantity")
            }
        }

        // Возвращаем только первые limit уровней
        return levelMap.values.toList()//.take(limit)
    }

    private fun calculateTotals(levels: List<OrderBookLevel>, isAsk: Boolean): List<OrderBookLevel> {
        var total = 0.0
        // Не сортируем! Binance уже прислал в правильном порядке
        return levels.map { level ->
            val qty = level.quantity.toDoubleOrNull() ?: 0.0
            total += qty
            level.copy(total = total.toString())
        }
    }
}