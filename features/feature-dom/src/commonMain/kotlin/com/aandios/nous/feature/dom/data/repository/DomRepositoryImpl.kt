package com.aandios.nous.feature.dom.data.repository

import com.aandios.nous.api.market.adapters.BookTickerAdapter
import com.aandios.nous.api.market.adapters.DomAdapter
import com.aandios.nous.api.market.model.BookTicker
import com.aandios.nous.api.market.model.orderbook.DepthSnapshot
import com.aandios.nous.api.market.model.orderbook.OrderBook
import com.aandios.nous.api.market.model.orderbook.OrderBookLevel
import com.aandios.nous.core.domain.repository.DomRepository
import com.aandios.nous.api.market.model.orderbook.OrderBookState
import com.aandios.nous.feature.dom.domain.model.DomEvent
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlin.math.pow

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

                val depthFlow = domAdapter.subscribeToDepthUpdates(symbol, depth)
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

                val delayMs = (1000 * 2.0.pow(reconnectAttempts - 1.0)).toLong()
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

    /**
     * Подписывается на инкрементальные события DOM (стакана котировок).
     * Вместо публикации полного OrderBook при каждом изменении,
     * этот поток эмитит события для добавления/обновления/удаления уровней.
     * 
     * @param symbol торговый символ
     * @param depth глубина стакана
     * @return поток событий DomEvent
     */
    suspend fun subscribeToDomEvents(symbol: String, depth: Int): Flow<DomEvent> = callbackFlow {
        println("📊 Subscribing to $symbol DOM events with depth $depth")

        val state = OrderBookState()
        var reconnectAttempts = 0
        val maxReconnectAttempts = 5

        while (true) {
            try {
                // Шаг 1: Получаем начальный снапшот и отправляем событие Snapshot
                val snapshot: DepthSnapshot = domAdapter.getOrderBookSnapshot(symbol, depth)
                state.updateFromSnapshot(snapshot)
                println("✅ Got snapshot for $symbol with ${state.bids.size} bids, ${state.asks.size} asks")
                
                // Отправляем событие Snapshot
                trySend(DomEvent.fromSnapshot(snapshot, symbol))

                // Шаг 2: Запускаем два параллельных потока
                val bookTickerFlow = bookTickerAdapter.subscribeToBookTicker(symbol)
                    .catch { e -> println("⚠️ BestPrices error: ${e.message}") }

                val depthFlow = domAdapter.subscribeToDepthUpdates(symbol, depth)
                    .catch { e -> println("⚠️ Depth updates error: ${e.message}") }

                // Шаг 3: Объединяем потоки и преобразуем в события
                bookTickerFlow.combine(depthFlow) { bookTicker, depthUpdate ->
                    // Применяем обновление depth к состоянию
                    state.applyUpdate(depthUpdate)
                    
                    // Отправляем события BestPrices
                    trySend(DomEvent.fromBookTicker(bookTicker, symbol))
                    
                    // Отправляем события для каждого изменения в depthUpdate
                    DomEvent.fromDepthUpdate(depthUpdate, symbol).forEach { event ->
                        trySend(event)
                    }
                    
                    // Возвращаем Unit, так как события отправляются через trySend
                    Unit
                }.collect {
                    reconnectAttempts = 0  // сброс счетчика при успехе
                }

            } catch (e: Exception) {
                println("❌ Error in $symbol DOM events: ${e.message}")
                e.printStackTrace()

                reconnectAttempts++
                if (reconnectAttempts > maxReconnectAttempts) {
                    println("❌ Max reconnection attempts reached for $symbol")
                    close(e)
                    break
                }

                val delayMs = (1000 * 2.0.pow(reconnectAttempts - 1.0)).toLong()
                println("⏳ Reconnecting to $symbol in ${delayMs}ms (attempt $reconnectAttempts)")
                delay(delayMs)
                
                // Отправляем событие Reset перед переподключением
                trySend(DomEvent.Reset)
            }
        }

        close()
    }
}
