package com.aandios.nous_platform.data.api.binance.models

import com.aandios.nous_platform.domain.entities.Candle
import kotlinx.serialization.Serializable

@Serializable
data class BinanceCandle(
    val openTime: Long,
    val open: String,
    val high: String,
    val low: String,
    val close: String,
    val volume: String,
    val closeTime: Long,
    val quoteAssetVolume: String,
    val numberOfTrades: Int,
    val takerBuyBaseAssetVolume: String,
    val takerBuyQuoteAssetVolume: String
) {
    fun toDomain(): Candle {
        return Candle(
            open = open.toFloat(),
            high = high.toFloat(),
            low = low.toFloat(),
            close = close.toFloat(),
            timestamp = openTime,
            volume = volume.toFloat(),

            )
    }
}
