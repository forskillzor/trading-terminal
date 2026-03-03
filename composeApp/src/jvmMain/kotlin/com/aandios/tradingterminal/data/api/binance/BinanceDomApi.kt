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

            val bids = response.bids.map { bid ->
                OrderBookLevel(
                    price = bid[0],
                    quantity = bid[1],
                    total = "0"
                )
            }

            val asks = response.asks.map { ask ->
                OrderBookLevel(
                    price = ask[0],
                    quantity = ask[1],
                    total = "0"
                )
            }

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
        var snapshotLastUpdateId = 0L
        var isWaitingForFirstValidUpdate = true
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

                                // Если еще не получили снапшот
                                if (localOrderBook == null) {
                                    localOrderBook = getOrderBookSnapshot(symbol)
                                    snapshotLastUpdateId = localOrderBook!!.lastUpdateId

                                    println("📊 DOM: Snapshot loaded with lastUpdateId: $snapshotLastUpdateId")
                                    println("📊 DOM: First update - U:${update.firstUpdateId}, u:${update.lastUpdateId}, pu:${update.prevLastUpdateId}")
                                }

                                // Правильная логика синхронизации с Binance Futures:
                                // 1. Сохраняем снапшот с lastUpdateId = snapshotId
                                // 2. Обрабатываем обновления где u <= snapshotId (могут быть старые)
                                // 3. Когда получаем обновление с u > snapshotId - начинаем применять все последующие

                                if (isWaitingForFirstValidUpdate) {
                                    if (update.lastUpdateId <= snapshotLastUpdateId) {
                                        // Пропускаем старые обновления
                                        if (updateCount % 10 == 0) {
                                            println("⏳ DOM: Still waiting for update > $snapshotLastUpdateId (current u=${update.lastUpdateId})")
                                        }
                                        continue
                                    }

                                    // Первое валидное обновление!
                                    println("✅ DOM: Found first valid update with u=${update.lastUpdateId}")
                                    isWaitingForFirstValidUpdate = false
                                }

                                // Применяем все обновления после первого валидного
                                if (localOrderBook != null && !isWaitingForFirstValidUpdate) {
                                    // Для фьючерсов применяем все обновления, даже с u <= snapshotLastUpdateId
                                    // после того как получили первое валидное
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
                                    snapshotLastUpdateId = update.lastUpdateId
                                    trySend(localOrderBook!!)
//                                    println("updated dom data: bid:${localOrderBook.bids.firstOrNull()}, ask:${localOrderBook.asks.firstOrNull()}")
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
        val newBids = updateLevels(current.bids, update.bids)
        val newAsks = updateLevels(current.asks, update.asks)

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
        val levelMap = LinkedHashMap<String, OrderBookLevel>()

        currentLevels.forEach { level ->
            levelMap[level.price] = level
        }

        updates.forEach { update ->
            val price = update[0]
            val quantity = update[1]
            val qtyDouble = quantity.toDoubleOrNull() ?: 0.0

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

        return levelMap.values.take(limit)
    }

    private fun calculateTotals(levels: List<OrderBookLevel>, isAsk: Boolean): List<OrderBookLevel> {
        var total = 0.0
        return levels.map { level ->
            val qty = level.quantity.toDoubleOrNull() ?: 0.0
            total += qty
            level.copy(total = total.toString())
        }
    }
}