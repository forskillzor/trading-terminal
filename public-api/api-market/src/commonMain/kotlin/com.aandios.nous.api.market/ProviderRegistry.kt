package com.aandios.nous.api.market

import com.aandios.nous.api.market.adapters.AdapterType

/**
 * Registry for managing and accessing market providers
 */
interface ProviderRegistry {
    /**
     * Get all available provider factories
     */
    fun getAllFactories(): List<ProviderFactory>

    /**
     * Get provider factory by ID
     */
    fun getFactory(providerId: String): ProviderFactory?

    /**
     * Get providers that support specific adapter type
     */
    fun getFactoriesByAdapterType(adapterType: AdapterType): List<ProviderFactory>

    /**
     * Create a provider instance
     */
    fun createProvider(
        providerId: String,
        config: ProviderConfig,
        networkManager: NetworkManager
    ): Provider?
}