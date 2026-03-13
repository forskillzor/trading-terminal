package com.aandios.nous.core.plugin

import com.aandios.nous.api.market.ProviderFactory
import java.io.File
import java.net.URLClassLoader
import java.util.ServiceLoader

class PluginLoader {

    /**
     * Загружает все плагины из папки plugins/
     */
    fun loadPluginsFromDirectory(pluginsDir: String): List<ProviderFactory> {
        val dir = File(pluginsDir)
        if (!dir.exists() || !dir.isDirectory) {
            println("Plugins directory not found: $pluginsDir")
            return emptyList()
        }

        // Находим все JAR файлы в папке plugins/
        val jarFiles = dir.listFiles { file -> file.extension == "jar" }?.toList() ?: emptyList()

        if (jarFiles.isEmpty()) {
            println("No JAR files found in $pluginsDir")
            return emptyList()
        }

        println("Found ${jarFiles.size} plugin JARs: ${jarFiles.joinToString { it.name }}")

        // Создаем изолированный ClassLoader для каждого JAR
        return jarFiles.flatMap { jarFile ->
            loadPluginsFromJar(jarFile)
        }
    }

    /**
     * Загружает плагины из конкретного JAR файла
     */
    private fun loadPluginsFromJar(jarFile: File): List<ProviderFactory> {
        return try {
            // Создаем URL для JAR файла
            val jarUrl = jarFile.toURI().toURL()

            // Создаем изолированный ClassLoader
            // Важно: parent ClassLoader должен видеть наши API интерфейсы!
            val classLoader = URLClassLoader(
                arrayOf(jarUrl),
                // Parent ClassLoader должен знать об API интерфейсах
                this.javaClass.classLoader
            )

            // Используем ServiceLoader для поиска всех реализаций ProviderFactory в этом JAR
            val serviceLoader = ServiceLoader.load(
                ProviderFactory::class.java,
                classLoader
            )

            val factories = serviceLoader.toList()
            println("Loaded ${factories.size} factories from ${jarFile.name}: $factories")

            factories
        } catch (e: Exception) {
            println("Failed to load plugins from ${jarFile.name}: ${e.message}")
            e.printStackTrace()
            emptyList()
        }
    }

    /**
     * Загружает все провайдеры (из встроенных и из папки plugins/)
     */
    fun loadAllProviders(): List<ProviderFactory> {
        val factories = mutableListOf<ProviderFactory>()

        // 1. Сначала загружаем встроенные провайдеры (которые скомпилированы вместе с приложением)
        val builtInLoader = ServiceLoader.load(ProviderFactory::class.java)
        factories.addAll(builtInLoader.toList())

        // 2. Затем загружаем из папки plugins/
        val pluginsDir = System.getProperty("user.dir") + "/plugins"
        factories.addAll(loadPluginsFromDirectory(pluginsDir))

        return factories
    }
}