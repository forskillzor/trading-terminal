package com.aandios.nous.api.market.model.orderbook

import kotlinx.serialization.Serializable

@Serializable
data class OrderBook(
    val symbol: String,
    val bids: List<OrderBookLevel> = emptyList(),
    val asks: List<OrderBookLevel> = emptyList(),
    val lastUpdateId: Long = 0,
    val timestamp: Long = System.currentTimeMillis()
)
data class DomOrder(
    val id: String,
    val price: Double,
    val quantity: Double,
    val side: OrderSide,
    val type: OrderType = OrderType.LIMIT,
    val timestamp: Long = System.currentTimeMillis()
)

enum class OrderSide {
    BUY, SELL
}

enum class OrderType {
    LIMIT, MARKET
}
