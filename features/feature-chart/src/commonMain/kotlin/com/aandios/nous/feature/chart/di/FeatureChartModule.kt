package com.aandios.nous.feature.chart.di

import com.aandios.nous.api.market.NetworkManager
import com.aandios.nous.api.market.Provider
import com.aandios.nous.api.market.ProviderConfig
import com.aandios.nous.api.market.adapters.ChartAdapter
import com.aandios.nous.core.data.repository.ChartRepositoryImpl
import com.aandios.nous.core.di.coreModule
import com.aandios.nous.core.domain.repository.ChartRepository
import com.aandios.nous.feature.chart.ui.ChartViewModel
import com.aandios.nous.provider.binance.BinanceProviderFactory
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module

/**
 * Инициализация Koin ТОЛЬКО для превью/изолированного запуска ChartWindow.
 * - Останавливаем старый контекст
 * - Не включаем binanceProviderModule (чтобы не было конфликта)
 * - Создаём Provider вручную через фабрику
 */
fun initKoinForPreview() {
    stopKoin()
    startKoin {
        modules(
            coreModule,           // NetworkManager, HttpClient
            featureChartModule,   // Модуль фичи Chart
        )
    }
}

val featureChartModule = module {

    // 1. Конфигурация для превью (без ключей, основная сеть)
    single<ProviderConfig> {
        ProviderConfig(
            apiKey = null,
            secretKey = null,
            isTestnet = false,
            customSettings = emptyMap()
        )
    }

    // 2. Создаём Provider напрямую через фабрику
    single<Provider> {
        val config = get<ProviderConfig>()
        val networkManager = get<NetworkManager>()

        BinanceProviderFactory().createProvider(
            config = config,
            networkManager = networkManager
        )
    }

    // 3. Адаптер Chart из провайдера
    single<ChartAdapter> {
        get<Provider>().chart ?: error("Chart adapter not available")
    }

    // 4. Репозиторий Chart (используем готовый из platform-core)
    single<ChartRepository> {
        ChartRepositoryImpl(chartAdapter = get())
    }

    // 5. ViewModel
    factory {
        ChartViewModel(
            chartRepository = get()
        )
    }
}
