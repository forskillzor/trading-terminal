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
import java.util.concurrent.ConcurrentHashMap

class BinanceDomApi(
    private val client: HttpClient,
    private val limit: Int = 20
) {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }

    // Кэш для быстрого доступа к уровням
    private data class OrderBookCache(
        val bids: ConcurrentHashMap<String, OrderBookLevel>,
        val asks: ConcurrentHashMap<String, OrderBookLevel>,
        var lastUpdateId: Long
    )

    suspend fun getOrderBookSnapshot(symbol: String): OrderBook {
        try {
            val response: DepthResponse = client.get("https://fapi.binance.com/fapi/v1/depth") {
                url {
                    parameters.append("symbol", symbol)
                    parameters.append("limit", limit.toString())
                }
            }.body()

            // Сортируем правильно:
            // Bids: от большей цены к меньшей (лучший bid сверху)
            val bids = response.bids
                .map { bid -> OrderBookLevel(price = bid[0], quantity = bid[1], total = "0") }
                .sortedByDescending { it.price.toDouble() }

            // Asks: от меньшей цены к большей (лучший ask сверху)
            val asks = response.asks
                .map { ask -> OrderBookLevel(price = ask[0], quantity = ask[1], total = "0") }
                .sortedBy { it.price.toDouble() }

            println("📊 DOM Snapshot:")
            println("   - Best bid: ${bids.firstOrNull()?.price} @ ${bids.firstOrNull()?.quantity}")
            println("   - Best ask: ${asks.firstOrNull()?.price} @ ${asks.firstOrNull()?.quantity}")

            val bidsWithTotals = calculateTotals(bids)
            val asksWithTotals = calculateTotals(asks)

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

        var cache: OrderBookCache? = null
        var lastProcessedUpdateId = 0L
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

                                // Получаем снапшот если нужно
                                if (cache == null) {
                                    val snapshot = getOrderBookSnapshot(symbol)
                                    cache = OrderBookCache(
                                        bids = ConcurrentHashMap<String, OrderBookLevel>().apply {
                                            snapshot.bids.forEach { put(it.price, it) }
                                        },
                                        asks = ConcurrentHashMap<String, OrderBookLevel>().apply {
                                            snapshot.asks.forEach { put(it.price, it) }
                                        },
                                        lastUpdateId = snapshot.lastUpdateId
                                    )
                                    lastProcessedUpdateId = snapshot.lastUpdateId
                                    println("📊 DOM: Cache initialized with lastUpdateId: $lastProcessedUpdateId")
                                    trySend(snapshot)
                                    continue
                                }

                                // Проверяем что обновление новее последнего обработанного
                                if (update.lastUpdateId <= lastProcessedUpdateId) {
                                    if (updateCount % 100 == 0) {
                                        println("⏩ Skipping old update: ${update.lastUpdateId} <= $lastProcessedUpdateId")
                                    }
                                    continue
                                }

                                // Применяем обновление
                                val updated = applyUpdateToCache(cache!!, update)
                                lastProcessedUpdateId = update.lastUpdateId

                                // Проверяем изменились ли лучшие цены
                                val bestBid = updated.bids.firstOrNull()?.price
                                val bestAsk = updated.asks.firstOrNull()?.price

                                if (updateCount % 10 == 0) {
                                    println("📊 DOM Update #$updateCount: Bid=$bestBid, Ask=$bestAsk")
                                }

                                trySend(updated)

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

    private fun applyUpdateToCache(cache: OrderBookCache, update: BinanceDepthUpdate): OrderBook {
        println("📦 Processing update #${update.lastUpdateId}")
        println("   Bids in update: ${update.bids.size}")
        println("   Asks in update: ${update.asks.size}")

        // Покажем первые несколько обновлений для отладки
        if (update.bids.isNotEmpty()) {
            println("   First bid update: ${update.bids.first()}")
        }
        if (update.asks.isNotEmpty()) {
            println("   First ask update: ${update.asks.first()}")
        }

        var bidsChanged = false
        var asksChanged = false

        // Обновляем bids
        update.bids.forEach { bidUpdate ->
            val price = bidUpdate[0]
            val quantity = bidUpdate[1]
            val qtyDouble = quantity.toDoubleOrNull() ?: 0.0

            if (qtyDouble == 0.0) {
                if (cache.bids.containsKey(price)) {
                    cache.bids.remove(price)
                    bidsChanged = true
                    println("   🗑️ Removed bid at $price")
                }
            } else {
                val oldValue = cache.bids[price]
                cache.bids[price] = OrderBookLevel(price, quantity, "0")
                bidsChanged = true
                if (oldValue == null) {
                    println("   ➕ Added bid at $price = $quantity")
                } else if (oldValue.quantity != quantity) {
                    println("   🔄 Updated bid at $price: ${oldValue.quantity} → $quantity")
                }
            }
        }

        // Обновляем asks
        update.asks.forEach { askUpdate ->
            val price = askUpdate[0]
            val quantity = askUpdate[1]
            val qtyDouble = quantity.toDoubleOrNull() ?: 0.0

            if (qtyDouble == 0.0) {
                if (cache.asks.containsKey(price)) {
                    cache.asks.remove(price)
                    asksChanged = true
                    println("   🗑️ Removed ask at $price")
                }
            } else {
                val oldValue = cache.asks[price]
                cache.asks[price] = OrderBookLevel(price, quantity, "0")
                asksChanged = true
                if (oldValue == null) {
                    println("   ➕ Added ask at $price = $quantity")
                } else if (oldValue.quantity != quantity) {
                    println("   🔄 Updated ask at $price: ${oldValue.quantity} → $quantity")
                }
            }
        }

        // Сортируем и берем нужное количество
        val sortedBids = cache.bids.values
            .sortedByDescending { it.price.toDouble() }
            .take(limit)

        val sortedAsks = cache.asks.values
            .sortedBy { it.price.toDouble() }
            .take(limit)

        // Пересчитываем тоталы
        val bidsWithTotals = if (bidsChanged) calculateTotals(sortedBids) else sortedBids
        val asksWithTotals = if (asksChanged) calculateTotals(sortedAsks) else sortedAsks

        val result = OrderBook(
            symbol = update.symbol,
            bids = bidsWithTotals,
            asks = asksWithTotals,
            lastUpdateId = update.lastUpdateId,
            timestamp = update.eventTime
        )

        println("   ✅ Result: Best bid=${result.bids.firstOrNull()?.price}, Best ask=${result.asks.firstOrNull()?.price}")

        return result
    }

    private fun calculateTotals(levels: List<OrderBookLevel>): List<OrderBookLevel> {
        var total = 0.0
        return levels.map { level ->
            val qty = level.quantity.toDoubleOrNull() ?: 0.0
            total += qty
            level.copy(total = total.toString())
        }
    }
}