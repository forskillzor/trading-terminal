package com.aandios.nous.core.domain.repository

import com.aandios.nous.api.market.model.BookTicker
import com.aandios.nous.api.market.model.orderbook.DomEvent
import com.aandios.nous.api.market.model.orderbook.OrderBook
import kotlinx.coroutines.flow.Flow

interface DomRepository {
    suspend fun subscribeToDomEvents(symbol: String, depth: Int): Flow<DomEvent>
}