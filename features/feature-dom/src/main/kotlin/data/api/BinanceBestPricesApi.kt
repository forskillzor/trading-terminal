package com.aandios.nous_platform.data.api.binance

import com.aandios.nous_platform.data.api.binance.models.BinanceBookTicker
import io.ktor.client.*
import io.ktor.client.plugins.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.serialization.json.Json
import kotlinx.coroutines.delay

class BinanceBestPricesApi(
    private val client: HttpClient
) {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }

    fun subscribeToBookTicker(symbol: String): Flow<BinanceBookTicker> = callbackFlow {
        val streamName = "${symbol.lowercase()}@bookTicker"  // ← меняем на bookTicker
        val endpoint = "wss://fstream.binance.com/ws/$streamName"

        try {
            client.webSocket(urlString = endpoint) {

                for (frame in incoming) {
                    when (frame) {
                        is Frame.Text -> {
                            val text = frame.readText()

                            try {
                                val bookTicker = json.decodeFromString<BinanceBookTicker>(text)
                                trySend(bookTicker)

                            } catch (e: Exception) {
                                println("❌ BookTicker parse error: ${e.message}")
                            }
                        }
                        else -> {}
                    }
                }
            }
        } catch (e: Exception) {
            println("❌ BookTicker WebSocket failed: ${e.message}")
            e.printStackTrace()
            delay(1000)
            throw e
        }

        close()
    }
}