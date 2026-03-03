package com.aandios.nous_platform.domain.repository

import com.aandios.nous_platform.domain.entities.OrderBookData
import kotlinx.coroutines.flow.Flow

interface DomRepository {
    fun getOrderBook(symbol: String): Flow<OrderBookData>
}