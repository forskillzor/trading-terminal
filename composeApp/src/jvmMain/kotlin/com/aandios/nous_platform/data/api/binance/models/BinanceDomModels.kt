package com.aandios.nous_platform.data.api.binance.models

import com.aandios.nous_platform.domain.entities.OrderBook
import com.aandios.nous_platform.domain.entities.OrderBookLevel
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class BinanceDepthUpdate(
    @SerialName("e") val eventType: String = "",
    @SerialName("E") val eventTime: Long = 0,
    @SerialName("T") val transactionTime: Long = 0,
    @SerialName("s") val symbol: String = "",
    @SerialName("U") val firstUpdateId: Long = 0,
    @SerialName("u") val lastUpdateId: Long = 0,
    @SerialName("pu") val prevLastUpdateId: Long = 0,
    @SerialName("b") val bids: List<List<String>> = emptyList(),
    @SerialName("a") val asks: List<List<String>> = emptyList()
)

@Serializable
data class BinanceOrderBookLevel(
    val price: String,
    val quantity: String,
    val total: String = "0" // Будем вычислять
)

@Serializable
data class BinanceOrderBook(
    val symbol: String,
    val bids: List<BinanceOrderBookLevel> = emptyList(),
    val asks: List<BinanceOrderBookLevel> = emptyList(),
    val lastUpdateId: Long = 0,
    val timestamp: Long = System.currentTimeMillis()
) {
    fun toOrderBook(): OrderBook {

        return OrderBook(
            symbol = this.symbol,
            bids = this.bids.map {
                OrderBookLevel(
                    price = it.price,
                    quantity = it.quantity,
                    total = it.total
                )
            },
            asks = this.asks.map {
                OrderBookLevel(
                    price = it.price,
                    quantity = it.quantity,
                    total = it.total
                )
            },
            timestamp = this.timestamp
        )
    }
}

@Serializable
data class DepthResponse(
    val lastUpdateId: Long,
    val bids: List<List<String>>,
    val asks: List<List<String>>,
    val E: Long? = null, // Message output time
    val T: Long? = null  // Transaction time
)
