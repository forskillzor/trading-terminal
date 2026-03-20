package com.aandios.nous.core.domain.repository

import com.aandios.nous.api.market.model.BookTicker
import com.aandios.nous.api.market.model.orderbook.OrderBook
import kotlinx.coroutines.flow.Flow

interface DomRepository {
    suspend fun subscribeToOrderBook(symbol: String, depth: Int): Flow<OrderBook>
    fun getBookTicker(symbol: String): Flow<BookTicker>
}