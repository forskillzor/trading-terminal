package com.aandios.nous.core.domain.entities.trades

import kotlinx.serialization.Serializable

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
