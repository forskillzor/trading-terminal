package com.aandios.nous.core.domain.entities.dom

import kotlinx.serialization.Serializable

@Serializable
data class DomOrder(
    val id: String,
    val price: Double,
    val quantity: Double,
    val side: OrderSide,
    val type: OrderType = OrderType.LIMIT,
    val timestamp: Long = System.currentTimeMillis()
)
