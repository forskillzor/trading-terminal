package com.aandios.nous.feature.dom.data.repository

import com.aandios.nous.api.market.adapters.BookTickerAdapter
import com.aandios.nous.api.market.adapters.DomAdapter
import com.aandios.nous.api.market.model.BookTicker
import com.aandios.nous.api.market.model.orderbook.DepthSnapshot
import com.aandios.nous.api.market.model.orderbook.OrderBook
import com.aandios.nous.api.market.model.orderbook.OrderBookLevel
import com.aandios.nous.core.domain.repository.DomRepository
import com.aandios.nous.feature.dom.domain.OrderBookState
import com.aandios.nous.provider.binance.model.BinanceDepthSnapshot
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine

class DomRepositoryImpl(
    private val domAdapter: DomAdapter,
    private val bookTickerAdapter: BookTickerAdapter
) : DomRepository {

    override suspend fun subscribeToOrderBook(symbol: String, depth: Int): Flow<OrderBook> = callbackFlow {
        println("📊 Subscribing to $symbol order book with BestPrice integration")

        val state = OrderBookState()
        var reconnectAttempts = 0
        val maxReconnectAttempts = 5

        while (true) {
            try {
                // Шаг 1: Получаем начальный снапшот
                val snapshot: DepthSnapshot = domAdapter.getOrderBookSnapshot(symbol, depth)

                // Инициализируем состояние
                state.updateFromSnapshot(snapshot)
                println("✅ Got snapshot for $symbol with ${state.bids.size} bids, ${state.asks.size} asks")

                // Шаг 2: Запускаем два параллельных потока
                val bookTickerFlow = bookTickerAdapter.subscribeToBookTicker(symbol)
                    .catch { e -> println("⚠️ BestPrices error: ${e.message}") }

                val depthFlow = domAdapter.subscribeToDepthUpdates(symbol)
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
    override fun getBookTicker(symbol: String): Flow<BookTicker> {
        return bookTickerAdapter.subscribeToBookTicker(symbol)
    }
    private fun buildOrderBook(
        symbol: String,
        state: OrderBookState,
        bookTicker: BookTicker
    ): OrderBook {
        val bids = state.getBidsUpToBestBid(bookTicker.bestBid)
        val asks = state.getAsksFromBestAsk(bookTicker.bestAsk)

        return OrderBook(
            symbol = symbol,
            bids = calculateTotals(bids),
            asks = calculateTotals(asks),
            lastUpdateId = state.lastUpdateId,
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
