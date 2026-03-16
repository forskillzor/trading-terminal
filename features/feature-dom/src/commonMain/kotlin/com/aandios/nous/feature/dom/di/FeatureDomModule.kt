package com.aandios.nous.feature.dom.di

import com.aandios.nous.api.market.ProviderConfig
import com.aandios.nous.api.market.adapters.BookTickerAdapter
import com.aandios.nous.api.market.adapters.DomAdapter
import com.aandios.nous.provider.binance.adapter.BinanceBookTickerAdapter
import com.aandios.nous.provider.binance.adapter.BinanceDomAdapter
import com.aandios.nous.provider.binance.di.binanceProviderModule
import com.aandios.nous.core.domain.repository.DomRepository
import com.aandios.nous.core.domain.repository.BookTickerRepository
import com.aandios.nous.feature.dom.data.repository.DomRepositoryImpl
import com.aandios.nous.core.data.repository.BookTickerRepositoryImpl
import org.koin.dsl.module

val featureDomModule = module {
    // Default configuration for Binance provider (mainnet)
    single { ProviderConfig(isTestnet = false) }

    // Include the provider's own module (registers adapters)
    includes(binanceProviderModule)

    // Expose adapters as singletons (they will be created with the default config)
    single<DomAdapter> { get<BinanceDomAdapter>() }
    single<BookTickerAdapter> { get<BinanceBookTickerAdapter>() }

    // Repositories
    single<DomRepository> {
        DomRepositoryImpl(
            domAdapter = get()
        )
    }
    single<BookTickerRepository> {
        BookTickerRepositoryImpl(
            bookTicker = get()
        )
    }

    // Provide HttpClient (already defined in binanceProviderModule via factory parameters)
    // No need to redefine, as the adapters already depend on HttpClient via factory.
}