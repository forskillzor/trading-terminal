package com.aandios.nous.core.domain.repository

import com.aandios.nous.core.domain.entities.dom.OrderBook
import kotlinx.coroutines.flow.Flow

interface DomRepository {
    fun getOrderBook(symbol: String): Flow<OrderBook>
}