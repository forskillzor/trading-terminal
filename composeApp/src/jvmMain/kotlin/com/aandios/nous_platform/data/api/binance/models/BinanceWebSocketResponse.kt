package com.aandios.nous_platform.data.api.binance.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class BinanceWebSocketResponse(
    @SerialName("e") val eventType: String = "",  // "kline"
    @SerialName("E") val eventTime: Long = 0,     // Event time
    @SerialName("s") val symbol: String = "",     // Symbol
    @SerialName("k") val kline: BinanceKlineData  // Kline data
)