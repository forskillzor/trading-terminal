package com.aandios.nous.core.plugin

import com.aandios.nous.api.market.ProviderFactory
import com.aandios.nous.api.market.ProviderConfig
import com.aandios.nous.api.market.Provider
import com.aandios.nous.api.market.adapters.AdapterType
import com.aandios.nous.api.market.NetworkManager

class ProviderRegistry(private val pluginLoader: PluginLoader = PluginLoader()) {
    private val factories: List<ProviderFactory> by lazy { pluginLoader.loadAllProviders() }
    private val providerCache = mutableMapOf<String, Provider>()

    fun getFactory(providerId: String): ProviderFactory? {
        return factories.find { it.providerId == providerId }
    }

    fun getFactoriesByAdapterType(adapterType: AdapterType): List<ProviderFactory> {
        return factories.filter { adapterType in it.supportedAdapters }
    }

    suspend fun getProvider(
        providerId: String,
        config: ProviderConfig,
        networkManager: NetworkManager
    ): Provider? {
        val key = "$providerId-${config.hashCode()}"
        return providerCache[key] ?: run {
            val factory = getFactory(providerId) ?: return null
            val provider = factory.createProvider(config, networkManager)
            providerCache[key] = provider
            provider
        }
    }

    fun getAllFactories(): List<ProviderFactory> = factories

    fun clearCache() {
        providerCache.clear()
    }
}