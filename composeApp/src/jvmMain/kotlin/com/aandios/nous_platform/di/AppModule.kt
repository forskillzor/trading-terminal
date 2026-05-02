package com.aandios.nous_platform.di

import com.aandios.nous.api.market.NetworkManager
import com.aandios.nous.api.market.Provider
import com.aandios.nous.api.market.ProviderConfig
import com.aandios.nous.api.market.adapters.BookTickerAdapter
import com.aandios.nous.api.market.adapters.ChartAdapter
import com.aandios.nous.api.market.adapters.DomAdapter
import com.aandios.nous.api.market.adapters.SymbolInfoAdapter
import com.aandios.nous.api.market.adapters.TradesAdapter
import com.aandios.nous.core.data.repository.BookTickerRepositoryImpl
import com.aandios.nous.core.data.repository.ChartRepositoryImpl
import com.aandios.nous.core.data.repository.SymbolInfoRepositoryImpl
import com.aandios.nous.core.data.repository.TradesRepositoryImpl
import com.aandios.nous.core.di.coreModule
import com.aandios.nous.core.domain.repository.BookTickerRepository
import com.aandios.nous.core.domain.repository.ChartRepository
import com.aandios.nous.core.domain.repository.DomRepository
import com.aandios.nous.core.domain.repository.SymbolInfoRepository
import com.aandios.nous.core.domain.repository.TradesRepository
import com.aandios.nous.feature.chart.ui.ChartViewModel
import com.aandios.nous.feature.dom.data.repository.DomRepositoryImpl
import com.aandios.nous.feature.dom.ui.DomViewModel
import com.aandios.nous.feature.trades.ui.TradesViewModel
import com.aandios.nous.provider.binance.BinanceProviderFactory
import com.aandios.nous_platform.ui.terminalLayout.TerminalStateViewModel
import org.koin.core.context.startKoin
import org.koin.dsl.module

// Unified DI module using new feature-module classes
val appModule = module {

    // 1. Core (NetworkManager + HttpClient)
    includes(coreModule)

    // 2. Provider configuration and Provider instance (defined ONCE)
    single<ProviderConfig> {
        ProviderConfig(
            apiKey = null,
            secretKey = null,
            isTestnet = false,
            customSettings = emptyMap()
        )
    }

    single<Provider> {
        val config = get<ProviderConfig>()
        val networkManager = get<NetworkManager>()

        BinanceProviderFactory().createProvider(
            config = config,
            networkManager = networkManager
        )
    }

    // 3. Adapters from Provider
    single<ChartAdapter> {
        get<Provider>().chart ?: error("Chart adapter not available")
    }

    single<DomAdapter> {
        get<Provider>().dom ?: error("DOM adapter not available")
    }

    single<BookTickerAdapter> {
        get<Provider>().bookTicker ?: error("BookTicker adapter not available")
    }

    single<TradesAdapter> {
        get<Provider>().trades ?: error("Trades adapter not available")
    }

    single<SymbolInfoAdapter> {
        get<Provider>().symbolInfo ?: error("SymbolInfo adapter not available")
    }

    // 4. Repositories
    single<ChartRepository> {
        ChartRepositoryImpl(chartAdapter = get())
    }

    single<DomRepository> {
        DomRepositoryImpl(
            domAdapter = get(),
            bookTickerAdapter = get()
        )
    }

    single<BookTickerRepository> {
        BookTickerRepositoryImpl(bookTicker = get())
    }

    single<TradesRepository> {
        TradesRepositoryImpl(tradesAdapter = get())
    }

    single<SymbolInfoRepository> {
        SymbolInfoRepositoryImpl(symbolInfoAdapter = get())
    }

    // 5. ViewModels
    factory {
        ChartViewModel(
            chartRepository = get(),
            symbolInfoAdapter = get()
        )
    }

    factory {
        DomViewModel(
            domRepository = get(),
            symbolInfoRepository = get()
        )
    }

    factory {
        TradesViewModel(
            tradesRepository = get()
        )
    }

    factory {
        TerminalStateViewModel()
    }
}

// Simple initialization
fun initKoin() {
    startKoin {
        modules(appModule)
    }
}
