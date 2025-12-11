package com.aandios.tradingterminal.data.api.binance

import com.aandios.tradingterminal.data.api.binance.models.BinanceCandle
import com.aandios.tradingterminal.data.api.binance.models.BinanceWebSocketCandle
import com.aandios.tradingterminal.data.api.binance.models.BinanceWebSocketResponse
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.websocket.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.websocket.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.serialization.json.Json

class BinanceApi(
    private val client: HttpClient,
) {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }
    suspend fun getCandles(
        symbol: String,
        interval: String,
        limit: Int = 100
    ): List<BinanceCandle> {
        val response: List<List<String>> = client.get("https://api.binance.com/api/v3/klines") {
            url {
                parameters.append("symbol", symbol)
                parameters.append("interval", interval)
                parameters.append("limit", limit.toString())
            }
        }.body()

        return response.map { rawCandle ->
            BinanceCandle(
                openTime = rawCandle[0].toLong(),
                open = rawCandle[1],
                high = rawCandle[2],
                low = rawCandle[3],
                close = rawCandle[4],
                volume = rawCandle[5],
                closeTime = rawCandle[6].toLong(),
                quoteAssetVolume = rawCandle[7],
                numberOfTrades = rawCandle[8].toInt(),
                takerBuyBaseAssetVolume = rawCandle[9],
                takerBuyQuoteAssetVolume = rawCandle[10]
            )
        }
    }
    suspend fun subscribeToCandles(
        symbol: String,
        interval: String
    ): Flow<BinanceWebSocketCandle> = callbackFlow {
        println("📡 WebSocket: Starting for $symbol $interval")

        val streamName = "${symbol.lowercase()}@kline_${interval}"
        val endpoint = "wss://stream.binance.com:9443/ws/$streamName"

        println("🔗 Connecting to: $endpoint")

        try {
            client.webSocket(urlString = endpoint) {
                println("✅ WebSocket: CONNECTED to $endpoint")

                for (frame in incoming) {
                    when (frame) {
                        is Frame.Text -> {
                            val text = frame.readText()

                            // Debug: print first message
                            // println("📨 Raw: $text")

                            try {
                                // Парсим JSON
                                val wsResponse = json.decodeFromString<BinanceWebSocketResponse>(text)

                                // Проверяем что это действительно kline событие
                                if (wsResponse.eventType == "kline") {
                                    val kline = wsResponse.kline

                                    val webSocketCandle = BinanceWebSocketCandle(
                                        symbol = wsResponse.symbol,
                                        openTime = kline.startTime,
                                        closeTime = kline.endTime,
                                        open = kline.open,
                                        high = kline.high,
                                        low = kline.low,
                                        close = kline.close,
                                        volume = kline.volume,
                                        isClosed = kline.isClosed
                                    )

                                    // Отправляем только закрытые свечи или все?
                                    // Если нужно только закрытые:
                                    // if (kline.isClosed) {
                                    //     trySend(webSocketCandle)
                                    // }

                                    // Или все свечи (включая обновления текущей):
                                    trySend(webSocketCandle)

                                    // Debug output
                                    val status = if (kline.isClosed) "CLOSED" else "UPDATE"
                                }

                            } catch (e: Exception) {
                                // Игнорируем ошибки парсинга не-kline сообщений
                            }
                        }

                        // ... остальная обработка фреймов ...
                        else -> {}
                    }
                }
            }
        } catch (e: Exception) {
            println("❌ WebSocket failed: ${e.message}")
            e.printStackTrace()
            throw e
        }

        close()
    }
}