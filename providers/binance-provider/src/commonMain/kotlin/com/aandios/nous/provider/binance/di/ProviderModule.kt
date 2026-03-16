package com.aandios.nous.provider.binance.di

import com.aandios.nous.api.market.ProviderConfig
import com.aandios.nous.api.market.adapters.*
import com.aandios.nous.provider.binance.*
import com.aandios.nous.provider.binance.adapter.BinanceChartAdapter
import com.aandios.nous.provider.binance.adapter.BinanceDomAdapter
import com.aandios.nous.provider.binance.adapter.BinanceTradesAdapter
import com.aandios.nous.provider.binance.adapter.BinanceTradingAdapter
import com.aandios.nous.provider.binance.adapter.BinanceBookTickerAdapter
import org.koin.core.parameter.parametersOf
import org.koin.dsl.module

val binanceProviderModule = module {
    // Фабрики для адаптеров - каждый провайдер получает свой экземпляр
    factory { (config: ProviderConfig) ->
        BinanceTradesAdapter(
            httpClient = get(),
            config = config
        )
    }

    factory { (config: ProviderConfig) ->
        BinanceChartAdapter(
            client = get(),
            config = config
        )
    }

    factory { (config: ProviderConfig) ->
        BinanceBookTickerAdapter(
            client = get(),
            config = config
        )
    }

    factory { (config: ProviderConfig) ->
        BinanceDomAdapter(
            client = get(),
            config = config,
            bookTickerAdapter = get { parametersOf(config) }
        )
    }

    factory { (config: ProviderConfig) ->
        BinanceTradingAdapter(
            client = get(),
            config = config
        )
    }

    // Фабрика для самого провайдера
    factory { (config: ProviderConfig) ->
        val tradesAdapter: MarketAdapter = get { parametersOf(config) }
        val domAdapter: MarketAdapter = get { parametersOf(config) }
        val chartAdapter: MarketAdapter = get { parametersOf(config) }
        val tradingAdapter: MarketAdapter = get { parametersOf(config) }
        val bookTickerAdapter: MarketAdapter = get { parametersOf(config) }
        val adapters = mapOf(
            AdapterType.TRADES to tradesAdapter,
            AdapterType.DOM to domAdapter,
            AdapterType.CHART to chartAdapter,
            AdapterType.TRADING to tradingAdapter,
            AdapterType.BOOK_TICKER to bookTickerAdapter
        )
        BinanceProvider(
            providerId = "binance",
            providerName = "Binance Exchange",
            version = "1.0.0",
            adapters = adapters
        )
    }
}