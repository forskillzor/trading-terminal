package com.aandios.nous_platform.data.api.binance

import com.aandios.nous_platform.data.api.binance.models.BinanceDepthUpdate
import com.aandios.nous_platform.data.api.binance.models.DepthResponse
import com.aandios.nous_platform.data.api.binance.models.BinanceOrderBook
import com.aandios.nous_platform.data.api.binance.models.BinanceOrderBookLevel
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
        val bids: ConcurrentHashMap<String, BinanceOrderBookLevel>,
        val asks: ConcurrentHashMap<String, BinanceOrderBookLevel>,
        var lastUpdateId: Long
    )

    suspend fun getOrderBookSnapshot(symbol: String): BinanceOrderBook {
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
                .map { bid -> BinanceOrderBookLevel(price = bid[0], quantity = bid[1], total = "0") }
                .sortedByDescending { it.price.toDouble() }

            // Asks: от меньшей цены к большей (лучший ask сверху)
            val asks = response.asks
                .map { ask -> BinanceOrderBookLevel(price = ask[0], quantity = ask[1], total = "0") }
                .sortedBy { it.price.toDouble() }

            val bidsWithTotals = calculateTotals(bids)
            val asksWithTotals = calculateTotals(asks)

            return BinanceOrderBook(
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

    fun subscribeToOrderBook(symbol: String): Flow<BinanceOrderBook> = callbackFlow {
        val streamName = "${symbol.lowercase()}@depth${limit}@100ms"
        val endpoint = "wss://fstream.binance.com/ws/$streamName"

        var cache: OrderBookCache? = null
        var lastProcessedUpdateId = 0L
        var updateCount = 0

        try {
            client.webSocket(urlString = endpoint) {

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
                                        bids = ConcurrentHashMap<String, BinanceOrderBookLevel>().apply {
                                            snapshot.bids.forEach { put(it.price, it) }
                                        },
                                        asks = ConcurrentHashMap<String, BinanceOrderBookLevel>().apply {
                                            snapshot.asks.forEach { put(it.price, it) }
                                        },
                                        lastUpdateId = snapshot.lastUpdateId
                                    )
                                    lastProcessedUpdateId = snapshot.lastUpdateId
                                    trySend(snapshot)
                                    continue
                                }

                                // Проверяем что обновление новее последнего обработанного
                                if (update.lastUpdateId <= lastProcessedUpdateId) {
                                    continue
                                }

                                // Применяем обновление
                                val updated = applyUpdateToCache(cache!!, update)
                                lastProcessedUpdateId = update.lastUpdateId

                                // Проверяем изменились ли лучшие цены
                                val bestBid = updated.bids.firstOrNull()?.price
                                val bestAsk = updated.asks.firstOrNull()?.price

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

    private fun applyUpdateToCache(cache: OrderBookCache, update: BinanceDepthUpdate): BinanceOrderBook {
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
                }
            } else {
                cache.bids[price] = BinanceOrderBookLevel(price, quantity, "0")
                bidsChanged = true
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
                }
            } else {
                val oldValue = cache.asks[price]
                cache.asks[price] = BinanceOrderBookLevel(price, quantity, "0")
                asksChanged = true
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

        val result = BinanceOrderBook(
            symbol = update.symbol,
            bids = bidsWithTotals,
            asks = asksWithTotals,
            lastUpdateId = update.lastUpdateId,
            timestamp = update.eventTime
        )

        return result
    }

    private fun calculateTotals(levels: List<BinanceOrderBookLevel>): List<BinanceOrderBookLevel> {
        var total = 0.0
        return levels.map { level ->
            val qty = level.quantity.toDoubleOrNull() ?: 0.0
            total += qty
            level.copy(total = total.toString())
        }
    }
}