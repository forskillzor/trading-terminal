package com.aandios.nous.provider.binance.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/**
 * Ответ на запрос exchangeInfo от Binance.
 */
@Serializable
data class BinanceExchangeInfoResponse(
    @SerialName("timezone") val timezone: String,
    @SerialName("serverTime") val serverTime: Long,
    @SerialName("rateLimits") val rateLimits: List<BinanceRateLimit>,
    @SerialName("exchangeFilters") val exchangeFilters: List<JsonElement>,
    @SerialName("symbols") val symbols: List<BinanceSymbolInfo>,
)

@Serializable
data class BinanceRateLimit(
    @SerialName("rateLimitType") val rateLimitType: String,
    @SerialName("interval") val interval: String,
    @SerialName("intervalNum") val intervalNum: Int,
    @SerialName("limit") val limit: Int,
)

@Serializable
data class BinanceSymbolInfo(
    @SerialName("symbol") val symbol: String,
    @SerialName("status") val status: String,
    @SerialName("baseAsset") val baseAsset: String,
    @SerialName("baseAssetPrecision") val baseAssetPrecision: Int,
    @SerialName("quoteAsset") val quoteAsset: String,
    @SerialName("quotePrecision") val quotePrecision: Int,
    @SerialName("quoteAssetPrecision") val quoteAssetPrecision: Int,
    @SerialName("baseCommissionPrecision") val baseCommissionPrecision: Int,
    @SerialName("quoteCommissionPrecision") val quoteCommissionPrecision: Int,
    @SerialName("orderTypes") val orderTypes: List<String>,
    @SerialName("icebergAllowed") val icebergAllowed: Boolean,
    @SerialName("ocoAllowed") val ocoAllowed: Boolean,
    @SerialName("quoteOrderQtyMarketAllowed") val quoteOrderQtyMarketAllowed: Boolean,
    @SerialName("allowTrailingStop") val allowTrailingStop: Boolean,
    @SerialName("cancelReplaceAllowed") val cancelReplaceAllowed: Boolean,
    @SerialName("isSpotTradingAllowed") val isSpotTradingAllowed: Boolean,
    @SerialName("isMarginTradingAllowed") val isMarginTradingAllowed: Boolean,
    @SerialName("filters") val filters: List<BinanceSymbolFilter>,
    @SerialName("permissions") val permissions: List<String>,
    @SerialName("defaultSelfTradePreventionMode") val defaultSelfTradePreventionMode: String,
    @SerialName("allowedSelfTradePreventionModes") val allowedSelfTradePreventionModes: List<String>,
)

@Serializable
sealed class BinanceSymbolFilter {
    abstract val filterType: String
}

@Serializable
@SerialName("PRICE_FILTER")
data class BinancePriceFilter(
    @SerialName("filterType") override val filterType: String,
    @SerialName("minPrice") val minPrice: String,
    @SerialName("maxPrice") val maxPrice: String,
    @SerialName("tickSize") val tickSize: String,
) : BinanceSymbolFilter()

@Serializable
@SerialName("LOT_SIZE")
data class BinanceLotSizeFilter(
    @SerialName("filterType") override val filterType: String,
    @SerialName("minQty") val minQty: String,
    @SerialName("maxQty") val maxQty: String,
    @SerialName("stepSize") val stepSize: String,
) : BinanceSymbolFilter()

@Serializable
@SerialName("MARKET_LOT_SIZE")
data class BinanceMarketLotSizeFilter(
    @SerialName("filterType") override val filterType: String,
    @SerialName("minQty") val minQty: String,
    @SerialName("maxQty") val maxQty: String,
    @SerialName("stepSize") val stepSize: String,
) : BinanceSymbolFilter()

@Serializable
@SerialName("MIN_NOTIONAL")
data class BinanceMinNotionalFilter(
    @SerialName("filterType") override val filterType: String,
    @SerialName("minNotional") val minNotional: String,
    @SerialName("applyToMarket") val applyToMarket: Boolean,
    @SerialName("avgPriceMins") val avgPriceMins: Int,
) : BinanceSymbolFilter()

// Другие фильтры можно добавить по необходимости