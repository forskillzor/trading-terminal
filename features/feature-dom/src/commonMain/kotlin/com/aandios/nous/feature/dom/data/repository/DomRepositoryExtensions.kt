package com.aandios.nous.feature.dom.data.repository

import com.aandios.nous.api.market.model.BookTicker
import com.aandios.nous.api.market.model.orderbook.OrderBook
import com.aandios.nous.core.domain.repository.DomRepository
import com.aandios.nous.feature.dom.domain.UnifiedOrderBook
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

/**
 * Расширения для DomRepository, предоставляющие поток унифицированных данных стакана.
 * Бизнес-логика преобразования OrderBook в UnifiedOrderBook находится здесь,
 * что позволяет UI-слою получать готовые к отображению данные.
 */
suspend fun DomRepository.subscribeToUnifiedOrderBook(
    symbol: String,
    depth: Int
): Flow<UnifiedOrderBook> {
    // Комбинируем потоки OrderBook и BookTicker для создания UnifiedOrderBook
    val orderBookFlow = subscribeToOrderBook(symbol, depth)
    val bookTickerFlow = getBookTicker(symbol)
    
    return orderBookFlow.combine(bookTickerFlow) { orderBook, bookTicker ->
        UnifiedOrderBook.fromOrderBook(orderBook, bookTicker)
    }
}

/**
 * Альтернативная версия, которая использует только OrderBook (без BookTicker).
 * Может быть полезно, если BookTicker недоступен.
 */
suspend fun DomRepository.subscribeToUnifiedOrderBookFromOrderBookOnly(
    symbol: String,
    depth: Int
): Flow<UnifiedOrderBook> {
    return subscribeToOrderBook(symbol, depth).map { orderBook ->
        UnifiedOrderBook.fromOrderBook(orderBook, null)
    }
}