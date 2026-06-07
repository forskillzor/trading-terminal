package com.aandios.nous.provider.binance.model

import com.aandios.nous.api.market.model.liquidation.LiquidationOrder
import com.aandios.nous.api.market.model.trading.TradeSide
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Binance liquidation order WebSocket message.
 * Stream: {symbol}@forceOrder
 *
 * Example:
 * {
 *   "e":"forceOrder", "E":1689926021000, "o":{
 *     "s":"BTCUSDT", "S":"BUY", "o":"LIMIT",
 *     "f":"IMMEDIATE_OR_CANCEL", "q":"0.123", "p":"12345.67",
 *     "ap":"12345.67", "X":"FILLED", "l":"0.123",
 *     "z":"0.123", "T":1689926021000
 *   }
 * }
 */
@Serializable
data class BinanceLiquidationEvent(
    @SerialName("e") val eventType: String = "",
    @SerialName("E") val eventTime: Long = 0,
    @SerialName("o") val order: BinanceLiquidationOrderData = BinanceLiquidationOrderData()
)

@Serializable
data class BinanceLiquidationOrderData(
    @SerialName("s") val symbol: String = "",
    @SerialName("S") val side: String = "",          // BUY or SELL
    @SerialName("o") val orderType: String = "",     // LIMIT, MARKET
    @SerialName("f") val timeInForce: String = "",
    @SerialName("q") val quantity: String = "",
    @SerialName("p") val price: String = "",         // Order price
    @SerialName("ap") val averagePrice: String = "", // Average fill price
    @SerialName("X") val status: String = "",        // FILLED, CANCELED
    @SerialName("l") val lastFilledQuantity: String = "",
    @SerialName("z") val filledQuantity: String = "",
    @SerialName("T") val tradeTime: Long = 0
)

fun BinanceLiquidationOrderData.toLiquidationOrder(): LiquidationOrder {
    return LiquidationOrder(
        symbol = symbol,
        price = averagePrice.ifEmpty { price }.toDoubleOrNull() ?: 0.0,
        quantity = quantity.toDoubleOrNull() ?: 0.0,
        timestamp = tradeTime,
        side = when (side.uppercase()) {
            "BUY" -> TradeSide.BUY
            "SELL" -> TradeSide.SELL
            else -> TradeSide.BUY
        },
        orderType = orderType
    )
}
