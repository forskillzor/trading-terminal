package com.aandios.nous.core.domain.entities.dom

import kotlinx.serialization.Serializable

@Serializable
data class OrderBook(
    val symbol: String,
    val bids: List<OrderBookLevel>,
    val asks: List<OrderBookLevel>,
    val timestamp: Long
)

