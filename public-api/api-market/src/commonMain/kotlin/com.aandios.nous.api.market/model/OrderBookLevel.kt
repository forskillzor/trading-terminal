package com.aandios.nous.api.market.model

import kotlinx.serialization.Serializable

@Serializable
data class OrderBookLevel(
    val price: String,
    val quantity: String,
    val total: String = "0" // Будем вычислять
)
