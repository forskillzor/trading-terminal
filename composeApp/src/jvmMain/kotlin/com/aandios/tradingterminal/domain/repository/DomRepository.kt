package com.aandios.tradingterminal.domain.repository

import com.aandios.tradingterminal.domain.entities.OrderBookData
import kotlinx.coroutines.flow.Flow

interface DomRepository {
    fun getOrderBook(symbol: String): Flow<OrderBookData>
}