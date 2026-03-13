package com.aandios.nous.api.market.model

data class Candle(
    val open: Float,
    val high: Float,
    val close: Float,
    val low: Float,
    val timestamp: Long,
    val volume: Float = 0f
)
