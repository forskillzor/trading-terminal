package com.aandios.tradingterminal.data.api.binance.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class BinanceAggTrade(
    @SerialName("e") val eventType: String = "", // "aggTrade"
    @SerialName("E") val eventTime: Long = 0,    // Event time
    @SerialName("s") val symbol: String = "",    // Symbol
    @SerialName("a") val aggregatedTradeId: Long = 0, // Aggregate trade ID
    @SerialName("p") val price: String = "",     // Price
    @SerialName("q") val quantity: String = "",  // Quantity
    @SerialName("f") val firstTradeId: Long = 0, // First trade ID
    @SerialName("l") val lastTradeId: Long = 0,  // Last trade ID
    @SerialName("T") val tradeTime: Long = 0,    // Trade time
    @SerialName("m") val isBuyerMaker: Boolean = false, // Is the buyer the market maker?
    @SerialName("M") val ignore: Boolean = false // Ignore
)

@Serializable
data class Trade(
    val id: Long,
    val symbol: String,
    val price: Double,
    val quantity: Double,
    val timestamp: Long,
    val isBuyerMaker: Boolean, // true = продажа (красный), false = покупка (зеленый)
    val side: TradeSide
)

enum class TradeSide {
    BUY, SELL
}

@Serializable
data class TradesResponse(
    val trades: List<Trade>,
    val symbol: String,
    val timestamp: Long
)