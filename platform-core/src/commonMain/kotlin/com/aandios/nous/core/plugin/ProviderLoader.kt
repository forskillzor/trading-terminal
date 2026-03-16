// platform-core/src/main/kotlin/com/aandios/nous/core/plugin/ProviderLoader.kt
package com.aandios.nous.core.plugin

import com.aandios.nous.api.market.Provider
import com.aandios.nous.api.market.ProviderConfig
import com.aandios.nous.api.market.ProviderFactory
import com.aandios.nous.api.market.NetworkManager
import java.io.File
import java.net.URLClassLoader
import java.util.*

class ProviderLoader {

    private val factories = mutableMapOf<String, ProviderFactory>()

    init {
        loadProviders()
    }

    private fun loadProviders() {
        // Ищем JAR файлы в папке providers (рядом с приложением)
        val providersDir = File("providers")
        if (!providersDir.exists()) {
            providersDir.mkdirs()
            println("📁 Created providers directory")
            return
        }

        providersDir.listFiles { file -> file.extension == "jar" }?.forEach { jarFile ->
            loadProviderFromJar(jarFile)
        }
    }

    private fun loadProviderFromJar(jarFile: File) {
        try {
            println("📦 Loading provider: ${jarFile.name}")

            val classLoader = URLClassLoader(
                arrayOf(jarFile.toURI().toURL()),
                ProviderFactory::class.java.classLoader
            )

            ServiceLoader.load(ProviderFactory::class.java, classLoader).forEach { factory ->
                factories[factory.providerId.lowercase()] = factory
                println("  ✅ Loaded: ${factory.providerName} v${factory.version}")
            }

        } catch (e: Exception) {
            println("  ❌ Failed to load ${jarFile.name}: ${e.message}")
        }
    }

    suspend fun getProvider(providerId: String, config: ProviderConfig, networkManager: NetworkManager): Provider? {
        val factory = factories[providerId.lowercase()] ?: return null

        return try {
            factory.createProvider(config, networkManager)
        } catch (e: Exception) {
            println("❌ Failed to create provider: ${e.message}")
            null
        }
    }

    fun getAvailableProviders(): List<ProviderInfo> {
        return factories.values.map { factory ->
            ProviderInfo(
                id = factory.providerId,
                name = factory.providerName,
                version = factory.version
            )
        }
    }
}

data class ProviderInfo(
    val id: String,
    val name: String,
    val version: String
)