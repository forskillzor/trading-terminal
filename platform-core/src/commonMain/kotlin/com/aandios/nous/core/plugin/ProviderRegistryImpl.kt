package com.aandios.nous.core.plugin

import com.aandios.nous.api.market.*
import com.aandios.nous.api.market.adapters.AdapterType

class ProviderRegistryImpl(
    private val loader: ProviderLoader = SimpleProviderLoader()
) : ProviderRegistry {

    override fun getAllFactories(): List<ProviderFactory> = loader.getAllFactories()

    override fun getFactory(providerId: String): ProviderFactory? = loader.getFactory(providerId)

    override fun getFactoriesByAdapterType(adapterType: AdapterType): List<ProviderFactory> {
        return loader.getFactoriesByAdapterType(adapterType)
    }

    override fun createProvider(
        providerId: String,
        config: ProviderConfig,
        networkManager: NetworkManager
    ): Provider? = loader.createProvider(providerId, config, networkManager)
}