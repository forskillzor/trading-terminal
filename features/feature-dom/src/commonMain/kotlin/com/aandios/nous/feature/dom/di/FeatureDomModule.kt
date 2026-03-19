package com.aandios.nous.feature.dom.di

import com.aandios.nous.api.market.NetworkManager
import com.aandios.nous.api.market.Provider
import com.aandios.nous.api.market.ProviderConfig
import com.aandios.nous.core.data.repository.BookTickerRepositoryImpl
import com.aandios.nous.core.data.repository.DomRepositoryImpl
import com.aandios.nous.core.di.coreModule
import com.aandios.nous.core.domain.repository.BookTickerRepository
import com.aandios.nous.core.domain.repository.DomRepository
import com.aandios.nous.core.plugin.ProviderLoader
import com.aandios.nous.feature.dom.ui.DomViewModel
import com.aandios.nous.provider.binance.di.binanceProviderModule
import org.koin.dsl.module

val featureDomModule = module {

    // Включаем необходимые модули
    includes(coreModule, binanceProviderModule)

    // 1. Конфигурация провайдера (например, для Binance)
    single { ProviderConfig(isTestnet = false) }

    // 2. Получаем провайдер через загрузчик
    //    В реальном приложении здесь может быть выбор провайдера по ID
    single<Provider> {
        val loader = get<ProviderLoader>()
        val config = get<ProviderConfig>()
        val networkManager = get<NetworkManager>()

        // Загружаем все доступные фабрики
        val factories = loader.loadAllProviders()
        // Находим фабрику для Binance (по ID или имени)
        val binanceFactory = factories.find { it.providerId == "binance-nous" }
            ?: error("Binance provider factory not found")

        // Создаем провайдер
        binanceFactory.createProvider(config, networkManager)
    }

    // 3. Предоставляем репозитории, используя адаптеры от провайдера
    single<DomRepository> {
        DomRepositoryImpl(
            domAdapter = get<Provider>().dom // Берем адаптер DOM у провайдера
        )
    }

    single<BookTickerRepository> {
        BookTickerRepositoryImpl(
            bookTicker = get<Provider>().bookTicker // Берем адаптер BookTicker у провайдера
        )
    }

    // 4. ViewModel (используем factory, чтобы не сохранять состояние между пересозданиями)
    factory {
        DomViewModel(
            domRepository = get(),
            bookTickerRepository = get()
        )
    }
}