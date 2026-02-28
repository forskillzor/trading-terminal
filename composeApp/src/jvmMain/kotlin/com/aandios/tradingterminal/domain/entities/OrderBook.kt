package com.aandios.tradingterminal.domain.entities

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
data class OrderBookLevel(
    val price: String,
    val quantity: String,
    val total: String = "0",
    @Transient
    val stableId: String = "${price}-${System.nanoTime()}" // Уникальный ID
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is OrderBookLevel) return false
        return price == other.price // Сравниваем только по цене
    }

    override fun hashCode(): Int = price.hashCode()
}
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