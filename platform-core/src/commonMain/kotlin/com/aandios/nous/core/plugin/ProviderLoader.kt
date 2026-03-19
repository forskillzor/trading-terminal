// platform-core/src/main/kotlin/com/aandios/nous/core/plugin/ProviderLoader.kt
package com.aandios.nous.core.plugin

import com.aandios.nous.api.market.Provider
import com.aandios.nous.api.market.ProviderConfig
import com.aandios.nous.api.market.ProviderFactory
import com.aandios.nous.api.market.NetworkManager
import com.aandios.nous.provider.binance.BinanceProvider
import com.aandios.nous.provider.binance.BinanceProviderFactory
import java.io.File
import java.net.URLClassLoader
import java.util.*

class ProviderLoader {
    fun loadAllProviders(): List<ProviderFactory> {
        return listOf<ProviderFactory>(
            BinanceProviderFactory()
        )
    }
}