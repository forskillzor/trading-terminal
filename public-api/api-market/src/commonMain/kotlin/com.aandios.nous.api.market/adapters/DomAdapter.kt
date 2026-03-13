package com.aandios.nous.api.market.adapters

import com.aandios.nous.api.market.model.OrderBook
import kotlinx.coroutines.flow.Flow

interface DomAdapter {
    /**
     * Dom Snapshot
     */
    suspend fun getOrderBookSnapshot(symbol: String, depth: Int = 20): OrderBook

    /**
     * Subscribe to order book updates
     */
    fun subscribeToOrderBook(symbol: String, depth: Int = 20): Flow<OrderBook>
}