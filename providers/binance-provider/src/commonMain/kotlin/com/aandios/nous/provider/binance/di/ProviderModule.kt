package com.aandios.nous.provider.binance.di

import com.aandios.nous.api.market.ProviderConfig
import com.aandios.nous.api.market.adapters.*
import com.aandios.nous.provider.binance.*
import org.koin.core.parameter.parametersOf
import org.koin.dsl.module

val binanceProviderModule = module {

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
            config = config,
            networkManager = get(),
        )
    }
}