package com.aandios.nous.api.market.model

import kotlinx.serialization.Serializable

@Serializable
data class OrderBook(
    val symbol: String,
    val bids: List<OrderBookLevel> = emptyList(),
    val asks: List<OrderBookLevel> = emptyList(),
    val lastUpdateId: Long = 0,
    val timestamp: Long = System.currentTimeMillis()
)
