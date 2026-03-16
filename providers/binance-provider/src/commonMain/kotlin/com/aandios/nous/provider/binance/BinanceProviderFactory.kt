package com.aandios.nous.provider.binance

import com.aandios.nous.api.market.*
import com.aandios.nous.api.market.adapters.*
import com.aandios.nous.provider.binance.adapter.*

class BinanceProviderFactory : ProviderFactory {

    override val providerId = "binance"
    override val providerName = "Binance Exchange"
    override val version = "1.0.0"

    override val supportedAdapters: Set<AdapterType> = setOf(
        AdapterType.TRADES,
        AdapterType.DOM,
        AdapterType.BOOK_TICKER,
        AdapterType.CHART,
        AdapterType.TRADING
    )

    override suspend fun createProvider(
        config: ProviderConfig,
        networkManager: NetworkManager
    ): Provider {
        val client = networkManager.httpClient

        // BookTicker нужен для DOM
        val bookTickerAdapter = BinanceBookTickerAdapter(client, config)

        // Единственное место перечисления адаптеров
        val adapters = mapOf(
            AdapterType.TRADES to BinanceTradesAdapter(client, config),
            AdapterType.DOM to BinanceDomAdapter(client, config, bookTickerAdapter),
            AdapterType.BOOK_TICKER to bookTickerAdapter,
            AdapterType.CHART to BinanceChartAdapter(client, config),
            AdapterType.TRADING to BinanceTradingAdapter(client, config)
        )

        return BinanceProvider(
            providerId = providerId,
            providerName = providerName,
            version = version,
            adapters = adapters
        )
    }
}