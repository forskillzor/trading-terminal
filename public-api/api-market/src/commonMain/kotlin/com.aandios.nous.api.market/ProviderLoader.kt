package com.aandios.nous.api.market

import com.aandios.nous.api.market.adapters.AdapterType

interface ProviderLoader {
    fun getAllFactories(): List<ProviderFactory>
    fun getFactory(providerId: String): ProviderFactory?
    fun getFactoriesByAdapterType(adapterType: AdapterType): List<ProviderFactory>
    suspend fun createProvider(
        providerId: String,
        config: ProviderConfig,
        networkManager: NetworkManager
    ): Provider?
}