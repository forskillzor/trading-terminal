package com.aandios.nous.api.market.model.orderbook


data class DepthSnapshot(
     val lastUpdateId: Long,
     val bids: List<List<String>>,
     val asks: List<List<String>>
)
