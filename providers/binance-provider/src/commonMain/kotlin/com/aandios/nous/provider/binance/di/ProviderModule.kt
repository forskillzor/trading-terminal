package com.aandios.nous.provider.binance.di

import com.aandios.nous.api.market.ProviderConfig
import com.aandios.nous.api.market.adapters.*
import com.aandios.nous.provider.binance.*
import com.aandios.nous.provider.binance.adapter.BinanceChartAdapter
import com.aandios.nous.provider.binance.adapter.BinanceDomAdapter
import com.aandios.nous.provider.binance.adapter.BinanceTradesAdapter
import com.aandios.nous.provider.binance.adapter.BinanceTradingAdapter
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
            httpClient = get(),
            config = config
        )
    }

    factory { (config: ProviderConfig) ->
        BinanceDomAdapter(
            httpClient = get(),
            config = config
        )
    }

    factory { (config: ProviderConfig) ->
        BinanceTradingAdapter(
            httpClient = get(),
            config = config
        )
    }

    // Фабрика для самого провайдера
    factory { (config: ProviderConfig) ->
        BinanceProvider(
            config = config,
            tradesAdapter = get { parametersOf(config) },
            domAdapter = get { parametersOf(config) },
            chartAdapter = get { parametersOf(config) },
            tradingAdapter = get { parametersOf(config) }
        )
    }
}