package com.aandios.nous.core.plugin

import com.aandios.nous.api.market.*
import com.aandios.nous.api.market.adapters.AdapterType
import com.aandios.nous.provider.binance.BinanceProviderFactory

/**
 * Simple provider loader for development - uses built-in providers only
 * No classloader magic yet, just direct instantiation
 */
class SimpleProviderLoader : ProviderLoader {

    private val factories: List<ProviderFactory> = listOf(
        BinanceProviderFactory()
        // Add more built-in providers here as they are implemented
    )

    override fun getAllFactories(): List<ProviderFactory> = factories

    override fun getFactory(providerId: String): ProviderFactory? {
        return factories.find { it.providerId == providerId }
    }

    override fun getFactoriesByAdapterType(adapterType: AdapterType): List<ProviderFactory> {
        // For now, all providers support all adapters
        // In real implementation, you'd check what each provider supports
        return factories
    }

    override fun createProvider(
        providerId: String,
        config: ProviderConfig,
        networkManager: NetworkManager
    ): Provider? {
        val factory = getFactory(providerId) ?: return null
        return factory.createProvider(config, networkManager)
    }
}