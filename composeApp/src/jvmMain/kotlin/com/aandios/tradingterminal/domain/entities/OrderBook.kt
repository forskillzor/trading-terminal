package com.aandios.tradingterminal.domain.entities

import kotlinx.serialization.Serializable

@Serializable
data class OrderBookLevel(
    val price: Double,
    val quantity: Double,
    val total: Double
)

@Serializable
data class OrderBookData(
    val symbol: String,
    val bids: List<OrderBookLevel>,
    val asks: List<OrderBookLevel>,
    val timestamp: Long
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