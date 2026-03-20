package com.aandios.nous.provider.binance.model

import com.aandios.nous.api.market.model.orderbook.DepthUpdate
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class BDepthUpdate(
    @SerialName("U") val firstUpdateId: Long,
    @SerialName("u") val finalUpdateId: Long,
    @SerialName("b") val bids: List<List<String>>,
    @SerialName("a") val asks: List<List<String>>
) {
    fun toDepthUpdate() = DepthUpdate(
        firstUpdateId,
        finalUpdateId,
        bids,
        asks
    )
}