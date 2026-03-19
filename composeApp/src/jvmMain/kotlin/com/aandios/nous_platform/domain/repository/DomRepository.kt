package com.aandios.nous_platform.domain.repository

import com.aandios.nous_platform.domain.entities.OrderBook
import kotlinx.coroutines.flow.Flow

interface DomRepository {
    fun getOrderBook(symbol: String): Flow<OrderBook>
}