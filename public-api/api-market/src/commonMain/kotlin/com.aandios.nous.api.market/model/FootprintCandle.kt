package com.aandios.nous.api.market.model

import kotlinx.serialization.Serializable

@Serializable
data class FootprintLevel(
    val price: String,
    val bidVolume: String,
    val askVolume: String,
    val bidCount: Int = 0,
    val askCount: Int = 0
) {
    val priceFloat: Float get() = price.toFloatOrNull() ?: 0f
    val bidVolumeFloat: Float get() = bidVolume.toFloatOrNull() ?: 0f
    val askVolumeFloat: Float get() = askVolume.toFloatOrNull() ?: 0f
}

@Serializable
data class FootprintCandle(
    val exchange: String = "",
    val symbol: String = "",
    val timeframe: String = "",
    val startTime: Long = 0L,
    val endTime: Long = 0L,
    val totalTicks: Long = 0L,
    val minPrice: String = "0",
    val maxPrice: String = "0",
    val levels: List<FootprintLevel> = emptyList()
) {
    val open: Float get() = levels.firstOrNull()?.priceFloat ?: 0f
    val high: Float get() = maxPrice.toFloatOrNull() ?: 0f
    val low: Float get() = minPrice.toFloatOrNull() ?: 0f
    val close: Float get() = levels.lastOrNull()?.priceFloat ?: 0f
    val maxVolume: Float get() = levels.maxOfOrNull { maxOf(it.bidVolumeFloat, it.askVolumeFloat) } ?: 0f
}
