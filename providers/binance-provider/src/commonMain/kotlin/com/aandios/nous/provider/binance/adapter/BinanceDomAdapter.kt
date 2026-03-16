package com.aandios.nous.provider.binance.adapter

import com.aandios.nous.api.market.adapters.DomAdapter
import com.aandios.nous.api.market.ProviderConfig
import com.aandios.nous.api.market.model.BookTicker
import com.aandios.nous.api.market.model.OrderBook
import com.aandios.nous.api.market.model.OrderBookLevel
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.websocket.*
import io.ktor.client.request.*
import io.ktor.websocket.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.SerialName
import java.util.concurrent.ConcurrentHashMap

/**
 * Binance DOM Adapter
 *
 * Упрощенная архитектура:
 * - BestPrice (bookTicker) как источник правды для лучших цен
 * - Depth (depth) только для объемов на уровнях
 * - Хеш-таблица для хранения всех уровней (O(1) обновление)
 * - Фильтрация уровней по лучшим ценам при отдаче UI
 */
class BinanceDomAdapter(
    private val client: HttpClient,
    private val config: ProviderConfig,
    private val bookTickerAdapter: BinanceBookTickerAdapter,  // для получения BestPrice
    private val depthLimit: Int = 20  // количество отображаемых уровней
) : DomAdapter {

    private val json = Json { ignoreUnknownKeys = true }

    @Serializable
    private data class DepthSnapshot(
        @SerialName("lastUpdateId") val lastUpdateId: Long,
        @SerialName("bids") val bids: List<List<String>>,
        @SerialName("asks") val asks: List<List<String>>
    )

    @Serializable
    private data class DepthUpdate(
        @SerialName("U") val firstUpdateId: Long,
        @SerialName("u") val finalUpdateId: Long,
        @SerialName("b") val bids: List<List<String>>,
        @SerialName("a") val asks: List<List<String>>
    )

    /**
     * Состояние стакана - только хеш-таблицы цена -> объем
     */
    private class OrderBookState {
        val bids = ConcurrentHashMap<String, String>()  // цена -> объем
        val asks = ConcurrentHashMap<String, String>()  // цена -> объем
        var lastUpdateId: Long = 0

        fun updateFromSnapshot(snapshot: DepthSnapshot) {
            snapshot.bids.forEach { (price, qty) ->
                if (qty.toDoubleOrNull() != 0.0) bids[price] = qty
            }
            snapshot.asks.forEach { (price, qty) ->
                if (qty.toDoubleOrNull() != 0.0) asks[price] = qty
            }
            lastUpdateId = snapshot.lastUpdateId
        }

        fun applyUpdate(update: DepthUpdate) {
            // Обновляем bids
            update.bids.forEach { (price, qty) ->
                if (qty.toDoubleOrNull() == 0.0) {
                    bids.remove(price)      // удаляем уровень
                } else {
                    bids[price] = qty       // обновляем/добавляем
                }
            }

            // Обновляем asks
            update.asks.forEach { (price, qty) ->
                if (qty.toDoubleOrNull() == 0.0) {
                    asks.remove(price)      // удаляем уровень
                } else {
                    asks[price] = qty       // обновляем/добавляем
                }
            }

            lastUpdateId = update.finalUpdateId
        }

        /**
         * Получить bids, обрезанные по лучшей цене
         */
        fun getBidsUpToBestBid(bestBid: Double, limit: Int): List<OrderBookLevel> {
            return bids
                .filter { it.key.toDouble() <= bestBid }  // только цены <= bestBid
                .map { OrderBookLevel(it.key, it.value) }
                .sortedByDescending { it.price.toDouble() }
                .take(limit)
        }

        /**
         * Получить asks, обрезанные по лучшей цене
         */
        fun getAsksFromBestAsk(bestAsk: Double, limit: Int): List<OrderBookLevel> {
            return asks
                .filter { it.key.toDouble() >= bestAsk }  // только цены >= bestAsk
                .map { OrderBookLevel(it.key, it.value) }
                .sortedBy { it.price.toDouble() }
                .take(limit)
        }
    }

    override suspend fun getOrderBookSnapshot(symbol: String, depth: Int): OrderBook {
        // Получаем snapshot через REST
        val snapshot: DepthSnapshot = client.get("https://fapi.binance.com/fapi/v1/depth") {
            url {
                parameters.append("symbol", symbol)
                parameters.append("limit", "1000")  // максимум для снапшота
            }
        }.body()

        // Получаем текущие лучшие цены
        val bookTicker = bookTickerAdapter.getBookTickerRest(symbol)

        return if (bookTicker != null) {
            // Строим стакан с фильтрацией по лучшим ценам
            buildOrderBook(symbol, snapshot, bookTicker)
        } else {
            // Если нет BestPrice, просто сортируем
            OrderBook(
                symbol = symbol,
                bids = snapshot.bids
                    .map { OrderBookLevel(it[0], it[1]) }
                    .sortedByDescending { it.price.toDouble() }
                    .take(depthLimit)
                    .let { calculateTotals(it) },
                asks = snapshot.asks
                    .map { OrderBookLevel(it[0], it[1]) }
                    .sortedBy { it.price.toDouble() }
                    .take(depthLimit)
                    .let { calculateTotals(it) },
                lastUpdateId = snapshot.lastUpdateId,
                timestamp = System.currentTimeMillis()
            )
        }
    }

    override fun subscribeToOrderBook(symbol: String, depth: Int): Flow<OrderBook> = callbackFlow {
        println("📊 Subscribing to $symbol order book with BestPrice integration")

        val state = OrderBookState()
        var reconnectAttempts = 0
        val maxReconnectAttempts = 5

        while (true) {
            try {
                // Шаг 1: Получаем начальный снапшот
                // todo maybe change to RPI Depth???
                val snapshot: DepthSnapshot = client.get("https://fapi.binance.com/fapi/v1/depth") {
                    url {
                        parameters.append("symbol", symbol)
                        parameters.append("limit", "1000")
                    }
                }.body()

                // Инициализируем состояние
                state.updateFromSnapshot(snapshot)
                println("✅ Got snapshot for $symbol with ${state.bids.size} bids, ${state.asks.size} asks")

                // Шаг 2: Запускаем два параллельных потока
                val bookTickerFlow = bookTickerAdapter.subscribeToBookTicker(symbol)
                    .catch { e -> println("⚠️ BestPrices error: ${e.message}") }

                val depthFlow = subscribeToDepthUpdates(symbol)
                    .catch { e -> println("⚠️ Depth updates error: ${e.message}") }

                // Шаг 3: Объединяем потоки
                bookTickerFlow.combine(depthFlow) { bookTicker, depthUpdate ->
                    // Применяем обновление depth
                    state.applyUpdate(depthUpdate)

                    // Строим стакан, обрезая по лучшим ценам
                    buildOrderBook(symbol, state, bookTicker)

                }.collect { orderBook ->
                    trySend(orderBook)
                    reconnectAttempts = 0  // сброс счетчика при успехе
                }

            } catch (e: Exception) {
                println("❌ Error in $symbol order book: ${e.message}")
                e.printStackTrace()

                reconnectAttempts++
                if (reconnectAttempts > maxReconnectAttempts) {
                    println("❌ Max reconnection attempts reached for $symbol")
                    close(e)
                    break
                }

                val delayMs = (1000 * Math.pow(2.0, reconnectAttempts - 1.0)).toLong()
                println("⏳ Reconnecting to $symbol in ${delayMs}ms (attempt $reconnectAttempts)")
                delay(delayMs)
            }
        }

        close()
    }

    /**
     * Подписка на depth обновления
     */
    private fun subscribeToDepthUpdates(symbol: String): Flow<DepthUpdate> = callbackFlow {
        val streamName = "${symbol.lowercase()}@depth"
        val endpoint = if (config.isTestnet) {
            "wss://testnet.binance.vision/ws/$streamName"
        } else {
            "wss://fstream.binance.com/ws/$streamName"
        }

        client.webSocket(urlString = endpoint) {
            println("✅ Depth WebSocket connected for $symbol")

            for (frame in incoming) {
                if (frame is Frame.Text) {
                    val update = json.decodeFromString<DepthUpdate>(frame.readText())
                    trySend(update)
                }
            }
        }

        close()
    }

    /**
     * Построение стакана из состояния с фильтрацией по лучшим ценам
     */
    private fun buildOrderBook(
        symbol: String,
        state: OrderBookState,
        bookTicker: BookTicker
    ): OrderBook {
        val bids = state.getBidsUpToBestBid(bookTicker.bestBid, depthLimit)
        val asks = state.getAsksFromBestAsk(bookTicker.bestAsk, depthLimit)

        return OrderBook(
            symbol = symbol,
            bids = calculateTotals(bids),
            asks = calculateTotals(asks),
            lastUpdateId = state.lastUpdateId,
            timestamp = System.currentTimeMillis()
        )
    }

    /**
     * Построение стакана из снапшота (для getOrderBookSnapshot)
     */
    private fun buildOrderBook(
        symbol: String,
        snapshot: DepthSnapshot,
        bookTicker: BookTicker
    ): OrderBook {
        // Фильтруем bids
        val bids = snapshot.bids
            .filter { it[0].toDouble() <= bookTicker.bestBid }
            .map { OrderBookLevel(it[0], it[1]) }
            .sortedByDescending { it.price.toDouble() }
            .take(depthLimit)

        // Фильтруем asks
        val asks = snapshot.asks
            .filter { it[0].toDouble() >= bookTicker.bestAsk }
            .map { OrderBookLevel(it[0], it[1]) }
            .sortedBy { it.price.toDouble() }
            .take(depthLimit)

        return OrderBook(
            symbol = symbol,
            bids = calculateTotals(bids),
            asks = calculateTotals(asks),
            lastUpdateId = snapshot.lastUpdateId,
            timestamp = System.currentTimeMillis()
        )
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