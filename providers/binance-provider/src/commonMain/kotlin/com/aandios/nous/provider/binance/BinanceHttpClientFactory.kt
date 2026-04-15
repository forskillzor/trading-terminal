package com.aandios.nous.provider.binance

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.websocket.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import kotlin.time.Duration.Companion.milliseconds

/**
 * Фабрика для создания HttpClient специфичного для Binance API.
 * Отдельный клиент позволяет настроить специфичные для Binance параметры:
 * - classDiscriminator = "filterType" для полиморфной десериализации фильтров
 * - Binance-specific таймауты и настройки
 */
object BinanceHttpClientFactory {
    fun create(): HttpClient = HttpClient(CIO) {
        install(HttpTimeout) {
            requestTimeoutMillis = 30000
            connectTimeoutMillis = 15000
            socketTimeoutMillis = 30000
        }

        install(WebSockets) {
            pingInterval = 30000.milliseconds
            maxFrameSize = Long.MAX_VALUE
        }

        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
                encodeDefaults = true
                classDiscriminator = "filterType"  // Ключевое для десериализации BinanceSymbolFilter
            })
        }

        expectSuccess = false
    }
}