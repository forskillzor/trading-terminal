package com.aandios.nous.api.market

interface ProviderFactory {

    suspend fun createProvider(
        config: ProviderConfig,
        networkManager: NetworkManager
    ): Provider
}