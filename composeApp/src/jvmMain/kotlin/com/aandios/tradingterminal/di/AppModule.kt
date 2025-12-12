package com.aandios.tradingterminal.di

import com.aandios.tradingterminal.data.api.binance.BinanceApi
import com.aandios.tradingterminal.data.api.BybitApi
import com.aandios.tradingterminal.data.api.binance.BinanceDomApi
import com.aandios.tradingterminal.data.api.binance.BinanceTradesApi
import com.aandios.tradingterminal.data.repository.ChartRepositoryImpl
import com.aandios.tradingterminal.data.repository.DomRepositoryImpl
import com.aandios.tradingterminal.data.repository.TradesRepositoryImpl
import com.aandios.tradingterminal.domain.repository.ChartRepository
import com.aandios.tradingterminal.domain.repository.DomRepository
import com.aandios.tradingterminal.domain.repository.TradesRepository
import com.aandios.tradingterminal.domain.usecases.GetChartByTickerUseCase
import com.aandios.tradingterminal.domain.usecases.GetChartByTickerUseCaseImpl
import com.aandios.tradingterminal.ui.chart.ChartViewModel
import com.aandios.tradingterminal.ui.dom.DomViewModel
import com.aandios.tradingterminal.ui.trades.TradesViewModel
import io.ktor.client.HttpClient
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
        HttpClient {
            // ОБА плагина должны быть установлены
            install(WebSockets) {
                // Опциональные настройки
                maxFrameSize = Long.MAX_VALUE
            }

            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                    isLenient = true
                })
            }
        }
    }

    // 2. API clients
    single<BinanceApi> {
        BinanceApi(
            client = get()
        )
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
            domRepository = get()
        )
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
}


// Simple initialization
fun initKoin() {
    startKoin {
        modules(appModule)
    }
}