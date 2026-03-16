package com.aandios.nous.core.domain.repository

import com.aandios.nous.api.market.model.OrderBook
import kotlinx.coroutines.flow.Flow

interface DomRepository {
    fun getOrderBook(symbol: String): Flow<OrderBook>
}