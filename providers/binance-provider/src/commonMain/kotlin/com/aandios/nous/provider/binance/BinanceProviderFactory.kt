package com.aandios.nous.provider.binance

import com.aandios.nous.api.market.*
import com.aandios.nous.provider.binance.adapter.*

class BinanceProviderFactory() : ProviderFactory {

    override suspend fun createProvider(
        config: ProviderConfig,
        networkManager: NetworkManager
    ): Provider {
        val client = networkManager.httpClient

        val bookTickerAdapter = BinanceBookTickerAdapter(client, config)


        return BinanceProvider(
            config = config,
            networkManager = networkManager,
        )
    }
}