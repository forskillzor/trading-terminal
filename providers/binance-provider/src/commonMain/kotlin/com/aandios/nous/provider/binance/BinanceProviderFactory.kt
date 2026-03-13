package com.aandios.nous.provider.binance

import com.aandios.nous.api.market.AdapterType
import com.aandios.nous.api.market.NetworkManager
import com.aandios.nous.api.market.Provider
import com.aandios.nous.api.market.ProviderConfig
import com.aandios.nous.api.market.ProviderFactory
import com.aandios.nous.provider.binance.adapter.BinanceChartAdapter
import com.aandios.nous.provider.binance.adapter.BinanceDomAdapter
import com.aandios.nous.provider.binance.adapter.BinanceTradesAdapter
import com.aandios.nous.provider.binance.adapter.BinanceTradingAdapter

class BinanceProviderFactory : ProviderFactory {

    override val providerId = "binance"
    override val providerName = "Binance Exchange"
    override val version = "1.0.0"

    override val supportedAdapters: Set<AdapterType> = setOf(
        AdapterType.TRADES,
        AdapterType.DOM,
        AdapterType.CHART,
        AdapterType.TRADING
    )

    override suspend fun createProvider(config: ProviderConfig, networkManager: NetworkManager): Provider {
        val httpClient = networkManager.httpClient
        return BinanceProvider(
            networkManager = networkManager,
            config = config,
            tradesAdapter = BinanceTradesAdapter(httpClient, config),
            domAdapter = BinanceDomAdapter(httpClient, config),
            chartAdapter = BinanceChartAdapter(httpClient, config),
            tradingAdapter = BinanceTradingAdapter(httpClient, config)
        )
    }
}