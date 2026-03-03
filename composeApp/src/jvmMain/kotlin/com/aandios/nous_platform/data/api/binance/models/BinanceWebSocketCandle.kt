package com.aandios.nous_platform.data.api.binance.models

import com.aandios.nous_platform.domain.entities.Candle

data class BinanceWebSocketCandle(
    val symbol: String,
    val openTime: Long,
    val closeTime: Long,
    val open: String,
    val high: String,
    val low: String,
    val close: String,
    val volume: String,
    val isClosed: Boolean
) {
    fun toDomain(): Candle {
        return Candle(
            open = open.toFloatOrNull() ?: 0f,
            high = high.toFloatOrNull() ?: 0f,
            low = low.toFloatOrNull() ?: 0f,
            close = close.toFloatOrNull() ?: 0f,
            timestamp = openTime,
            volume = volume.toFloatOrNull() ?: 0f
        )
    }
}