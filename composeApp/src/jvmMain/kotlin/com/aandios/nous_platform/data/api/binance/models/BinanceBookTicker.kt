package com.aandios.nous_platform.data.api.binance.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class BinanceBookTicker(
    @SerialName("e") val eventType: String,        // "bookTicker"
    @SerialName("u") val updateId: Long,           // order book updateId
    @SerialName("E") val eventTime: Long,          // event time
    @SerialName("T") val transactionTime: Long,    // transaction time
    @SerialName("s") val symbol: String,           // symbol
    @SerialName("b") val bestBidPrice: String,     // best bid price
    @SerialName("B") val bestBidQty: String,       // best bid quantity
    @SerialName("a") val bestAskPrice: String,     // best ask price
    @SerialName("A") val bestAskQty: String        // best ask quantity
) {
    fun toDomain(): BestPrices {
        return BestPrices(
            symbol = symbol,
            bestBid = bestBidPrice.toDouble(),
            bestBidQty = bestBidQty.toDouble(),
            bestAsk = bestAskPrice.toDouble(),
            bestAskQty = bestAskQty.toDouble(),
            lastPrice = (bestBidPrice.toDouble() + bestAskPrice.toDouble()) / 2, // approximate last price
            timestamp = eventTime
        )
    }
}

data class BestPrices(
    val symbol: String,
    val bestBid: Double,
    val bestBidQty: Double,
    val bestAsk: Double,
    val bestAskQty: Double,
    val lastPrice: Double,
    val timestamp: Long
) {
    val spread: Double get() = bestAsk - bestBid
    val spreadPercent: Double get() = if (bestBid > 0) (spread / bestBid) * 100 else 0.0
}