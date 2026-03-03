package com.aandios.nous_platform.domain.entities

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

val mockDomData = OrderBookData(
    symbol = "BYTECOIN",
    bids = listOf(
        OrderBookLevel("99.9", "1043", "20", ""),
        OrderBookLevel("99.8", "456", "20", ""),
        OrderBookLevel("99.7", "854", "20", ""),
        OrderBookLevel("99.6", "432", "20", ""),
        OrderBookLevel("99.5", "85", "20", ""),
        OrderBookLevel("99.4", "953", "20", ""),
        OrderBookLevel("99.3", "43", "20", ""),
        OrderBookLevel("99.2", "532", "20", ""),
        OrderBookLevel("99.1", "23", "20", ""),
        OrderBookLevel("99.0", "12", "20", ""),
        OrderBookLevel("98.9", "1043", "20", ""),
        OrderBookLevel("98.8", "456", "20", ""),
        OrderBookLevel("98.7", "854", "20", ""),
        OrderBookLevel("98.6", "432", "20", ""),
        OrderBookLevel("98.5", "85", "20", ""),
        OrderBookLevel("98.4", "953", "20", ""),
        OrderBookLevel("98.3", "43", "20", ""),
        OrderBookLevel("98.2", "532", "20", ""),
        OrderBookLevel("98.1", "23", "20", ""),
        OrderBookLevel("90.0", "12", "20", ""),
    ),
    asks = listOf(
        OrderBookLevel("100.0", "3452", "10", ""),
        OrderBookLevel("100.1", "311", "10", ""),
        OrderBookLevel("100.2", "567", "10", ""),
        OrderBookLevel("100.3", "876", "10", ""),
        OrderBookLevel("100.4", "543", "10", ""),
        OrderBookLevel("100.5", "234", "10", ""),
        OrderBookLevel("100.6", "125", "10", ""),
        OrderBookLevel("100.7", "865", "10", ""),
        OrderBookLevel("100.8", "32", "10", ""),
        OrderBookLevel("100.9", "65", "10", ""),
        OrderBookLevel("101.0", "3452", "10", ""),
        OrderBookLevel("101.1", "311", "10", ""),
        OrderBookLevel("101.2", "567", "10", ""),
        OrderBookLevel("101.3", "876", "10", ""),
        OrderBookLevel("101.4", "543", "10", ""),
        OrderBookLevel("101.5", "234", "10", ""),
        OrderBookLevel("101.6", "125", "10", ""),
        OrderBookLevel("101.7", "865", "10", ""),
        OrderBookLevel("101.8", "32", "10", ""),
        OrderBookLevel("101.9", "65", "10", ""),
    ),
    timestamp = 0L
)