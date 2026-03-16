package com.aandios.nous.api.market

import com.aandios.nous.api.market.adapters.*
import com.aandios.nous.api.market.model.Symbol

interface Provider {
    val providerId: String
    val providerName: String
    val version: String
    val adapters: Map<AdapterType, MarketAdapter>

    /**
     * Получить адаптер по enum (для случаев, когда тип известен только в рантайме)
     */
    @Suppress("UNCHECKED_CAST")
    fun <T : MarketAdapter> get(type: AdapterType): T? = adapters[type] as? T

    suspend fun isAvailable(): Boolean
    suspend fun getAvailableSymbols(): List<Symbol>

    suspend fun start() {}

    suspend fun stop() {}
}

/**
 * Получить адаптер по его типу (рекомендуемый способ)
 * Пример: val trades = provider.get<TradesAdapter>()
 */
inline fun <reified T : MarketAdapter> Provider.get(): T? {
    val type = when (T::class) {
        TradesAdapter::class -> AdapterType.TRADES
        DomAdapter::class -> AdapterType.DOM
        BookTickerAdapter::class -> AdapterType.BOOK_TICKER
        ChartAdapter::class -> AdapterType.CHART
        TradingAdapter::class -> AdapterType.TRADING
        else -> return null
    }
    return adapters[type] as? T
}