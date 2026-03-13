package com.aandios.nous.core.domain.entities.dom

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
