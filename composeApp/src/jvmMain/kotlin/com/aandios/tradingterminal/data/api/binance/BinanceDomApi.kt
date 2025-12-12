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
            println("📊 DOM: Fetching snapshot for $symbol")

            // Binance Futures API возвращает простой JSON
            val response: DepthResponse = client.get("https://fapi.binance.com/fapi/v1/depth") {
                url {
                    parameters.append("symbol", symbol)
                    parameters.append("limit", limit.toString())
                }
            }.body()

            println("📊 DOM: Snapshot received, lastUpdateId: ${response.lastUpdateId}")
            println("📊 DOM: Bids: ${response.bids.size}, Asks: ${response.asks.size}")

            // Суммируем объемы для построения DOM
            var bidTotal = 0.0
            val bidsWithTotal = response.bids.map { bid ->
                val price = bid[0]
                val quantity = bid[1]
                val qty = quantity.toDouble()
                bidTotal += qty
                OrderBookLevel(price, quantity, bidTotal.toString())
            }.sortedByDescending { it.price.toDouble() }

            var askTotal = 0.0
            val asksWithTotal = response.asks.map { ask ->
                val price = ask[0]
                val quantity = ask[1]
                val qty = quantity.toDouble()
                askTotal += qty
                OrderBookLevel(price, quantity, askTotal.toString())
            }.sortedBy { it.price.toDouble() }

            return OrderBook(
                symbol = symbol,
                bids = bidsWithTotal,
                asks = asksWithTotal,
                lastUpdateId = response.lastUpdateId,
                timestamp = response.T ?: System.currentTimeMillis()
            )
        } catch (e: Exception) {
            println("❌ DOM: Failed to fetch order book: ${e.message}")
            e.printStackTrace()
            throw e
        }
    }

    fun subscribeToOrderBook(symbol: String, levels: Int = 20): Flow<OrderBook> = callbackFlow {
        val streamName = "${symbol.lowercase()}@depth${levels}@100ms"
        val endpoint = "wss://fstream.binance.com/ws/$streamName"

        println("🔗 DOM WebSocket: Connecting to $endpoint")

        var localOrderBook: OrderBook? = null

        try {
            client.webSocket(urlString = endpoint) {
                println("✅ DOM WebSocket: CONNECTED")

                for (frame in incoming) {
                    when (frame) {
                        is Frame.Text -> {
                            val text = frame.readText()

                            try {
                                val update = json.decodeFromString<BinanceDepthUpdate>(text)

                                // Если у нас еще нет локальной копии, получаем снапшот
                                if (localOrderBook == null) {
                                    localOrderBook = getOrderBookSnapshot(symbol)
                                }

                                localOrderBook = updateOrderBook(localOrderBook!!, update)

                                if (localOrderBook != null) {
                                    trySend(localOrderBook!!)
                                }
                            } catch (e: Exception) {
                                println("❌ DOM parsing error: ${e.message}")
                            }
                        }
                        else -> {}
                    }
                }
            }
        } catch (e: Exception) {
            println("❌ DOM WebSocket failed: ${e.message}")
            throw e
        }

        close()
    }

    private fun updateOrderBook(current: OrderBook, update: BinanceDepthUpdate): OrderBook {
        // Обновляем bids
        val updatedBids = updateBidsLevels(current.bids, update.bids)
        val updatedAsks = updateAsksLevels(current.asks, update.asks)

        // Пересчитываем тоталы
        var bidTotal = 0.0
        val bidsWithTotals = updatedBids.map {
            val qty = it.quantity.toDouble()
            bidTotal += qty
            it.copy(total = bidTotal.toString())
        }

        var askTotal = 0.0
        val asksWithTotals = updatedAsks.map {
            val qty = it.quantity.toDouble()
            askTotal += qty
            it.copy(total = askTotal.toString())
        }

        return current.copy(
            bids = bidsWithTotals,
            asks = asksWithTotals,
            lastUpdateId = update.lastUpdateId,
            timestamp = update.eventTime
        )
    }

    private fun updateBidsLevels(
        currentBids: List<OrderBookLevel>,
        updates: List<List<String>>
    ): List<OrderBookLevel> {
        val mutableBids = currentBids.toMutableList()

        updates.forEach { update ->
            val price = update[0]
            val quantity = update[1].toDouble()

            val existingIndex = mutableBids.indexOfFirst { it.price == price }

            if (quantity == 0.0) {
                // Удалить уровень
                if (existingIndex != -1) {
                    mutableBids.removeAt(existingIndex)
                }
            } else {
                // Обновить или добавить уровень
                val newLevel = OrderBookLevel(price, update[1])
                if (existingIndex != -1) {
                    mutableBids[existingIndex] = newLevel
                } else {
                    mutableBids.add(newLevel)
                }
            }
        }

        // Сортируем по убыванию цены и берем топ-N
        return mutableBids
            .sortedByDescending { it.price.toDouble() }
            .take(limit)
    }

    private fun updateAsksLevels(
        currentAsks: List<OrderBookLevel>,
        updates: List<List<String>>
    ): List<OrderBookLevel> {
        val mutableAsks = currentAsks.toMutableList()

        updates.forEach { update ->
            val price = update[0]
            val quantity = update[1].toDouble()

            val existingIndex = mutableAsks.indexOfFirst { it.price == price }

            if (quantity == 0.0) {
                // Удалить уровень
                if (existingIndex != -1) {
                    mutableAsks.removeAt(existingIndex)
                }
            } else {
                // Обновить или добавить уровень
                val newLevel = OrderBookLevel(price, update[1])
                if (existingIndex != -1) {
                    mutableAsks[existingIndex] = newLevel
                } else {
                    mutableAsks.add(newLevel)
                }
            }
        }

        // Сортируем по возрастанию цены и берем топ-N
        return mutableAsks
            .sortedBy { it.price.toDouble() }
            .take(limit)
    }
}