package com.aandios.nous.provider.binance.di

import com.aandios.nous.api.market.NetworkManager
import com.aandios.nous.api.market.Provider
import com.aandios.nous.api.market.ProviderConfig
import com.aandios.nous.provider.binance.BinanceProviderFactory
import org.koin.dsl.module

val binanceProviderModule = module {
    // Фабрика для создания провайдера
    single { BinanceProviderFactory() }

    // Провайдер создается с параметрами
    factory<Provider> { (config: ProviderConfig) ->
        val factory = get<BinanceProviderFactory>()
        val networkManager = get<NetworkManager>()
        factory.createProvider(config, networkManager)
    }
}