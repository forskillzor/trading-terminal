package com.aandios.nous.api.market

interface ProviderFactory {
    val providerId: String
    val providerName: String
    val providerVersion: String

    fun createProvider(
        config: ProviderConfig,
        networkManager: NetworkManager
    ): Provider
}