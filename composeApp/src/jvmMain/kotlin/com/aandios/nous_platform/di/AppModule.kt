package com.aandios.nous_platform.di

import com.aandios.nous_platform.data.api.binance.BinanceApi
import com.aandios.nous_platform.data.api.binance.BinanceBestPricesApi
import com.aandios.nous_platform.data.api.bybit.BybitApi
import com.aandios.nous_platform.data.api.binance.BinanceDomApi
import com.aandios.nous_platform.data.api.binance.BinanceTradesApi
import com.aandios.nous_platform.data.repository.BestPricesRepositoryImpl
import com.aandios.nous_platform.data.repository.ChartRepositoryImpl
import com.aandios.nous_platform.data.repository.DomRepositoryImpl
import com.aandios.nous_platform.data.repository.TradesRepositoryImpl
import com.aandios.nous_platform.domain.repository.BestPricesRepository
import com.aandios.nous_platform.domain.repository.ChartRepository
import com.aandios.nous_platform.domain.repository.DomRepository
import com.aandios.nous_platform.domain.repository.TradesRepository
import com.aandios.nous_platform.domain.usecases.GetChartByTickerUseCase
import com.aandios.nous_platform.domain.usecases.GetChartByTickerUseCaseImpl
import com.aandios.nous_platform.ui.chart.ChartViewModel
import com.aandios.nous_platform.ui.dom.DomViewModel
import com.aandios.nous_platform.ui.terminalLayout.TerminalStateViewModel
import com.aandios.nous_platform.ui.trades.TradesViewModel
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import org.koin.core.context.startKoin
import org.koin.dsl.module

// all in one, for beginning
val appModule = module {

    // 1. HTTP clients

    single {
        HttpClient(CIO) {  // Явно указываем движок
            // Таймауты для HTTP запросов
            install(HttpTimeout) {
                requestTimeoutMillis = 30000  // 30 секунд
                connectTimeoutMillis = 15000  // 15 секунд
                socketTimeoutMillis = 30000   // 30 секунд
            }

            // WebSocket плагин
            install(WebSockets) {
                pingInterval = 30000  // Пинг каждые 30 секунд
                maxFrameSize = Long.MAX_VALUE
            }

            // Content negotiation
            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                    isLenient = true
                    encodeDefaults = true
                })
            }
        }
    }

// 2. API clients
    single<BinanceApi> {
        BinanceApi(client = get())
    }

    single<BybitApi> {
        BybitApi(client = get())
    }

// 3. Repository
    single<ChartRepository> {
        ChartRepositoryImpl(
            binanceApi = get(),
            bybitApi = get()
        )
    }

    single<BinanceDomApi> {
        BinanceDomApi(client = get())
    }

// DOM Repository
    single<DomRepository> {
        DomRepositoryImpl(domApi = get())
    }

// DOM ViewModel
    factory {
        DomViewModel(
            domRepository = get(),
            bestPricesRepository = get(),
        )
    }

    factory {
        TerminalStateViewModel()
    }

// 4. Use Cases
    single<GetChartByTickerUseCase> {
        GetChartByTickerUseCaseImpl(
            repository = get()
        )
    }

// 5. ViewModels (factory - new instance for each screen)
    factory {
        ChartViewModel(
            getChartUseCase = get()
        )
    }
    single<BinanceTradesApi> {
        BinanceTradesApi(client = get())
    }

    single<TradesRepository> {
        TradesRepositoryImpl(tradesApi = get())
    }

    factory {
        TradesViewModel(
            tradesRepository = get()
        )
    }
    single<BinanceBestPricesApi> {
        BinanceBestPricesApi(client = get())
    }

    single<BestPricesRepository> {
        BestPricesRepositoryImpl(bestPricesApi = get())
    }
}


// Simple initialization
fun initKoin() {
    startKoin {
        modules(appModule)
    }
}