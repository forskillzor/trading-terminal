package com.aandios.nous.api.market

import com.aandios.nous.api.market.adapters.AdapterType

interface ProviderFactory {
    val providerId: String
    val providerName: String
    val version: String
    val supportedAdapters: Set<AdapterType>

    suspend fun createProvider(
        config: ProviderConfig,
        networkManager: NetworkManager
    ): Provider
}