package com.aandios.nous_platform.data.api.binance

import com.aandios.nous_platform.data.api.binance.models.BinanceAggTrade
import com.aandios.nous_platform.data.api.binance.models.Trade
import com.aandios.nous_platform.data.api.binance.models.TradeSide
import io.ktor.client.HttpClient
import io.ktor.client.plugins.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.serialization.json.Json

class BinanceTradesApi(
    private val client: HttpClient
) {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }

    fun subscribeToAggTrades(symbol: String, limit: Int = 100): Flow<Trade> = callbackFlow {
        val streamName = "${symbol.lowercase()}@aggTrade"
        val endpoint = "wss://fstream.binance.com/ws/$streamName"

        println("🔗 Trades WebSocket: Connecting to $endpoint")

        try {
            client.webSocket(urlString = endpoint) {
                println("✅ Trades WebSocket: CONNECTED to $endpoint")

                for (frame in incoming) {
                    when (frame) {
                        is Frame.Text -> {
                            val text = frame.readText()

                            try {
                                val aggTrade = json.decodeFromString<BinanceAggTrade>(text)

                                // Конвертируем в нашу модель
                                val trade = Trade(
                                    id = aggTrade.aggregatedTradeId,
                                    symbol = aggTrade.symbol,
                                    price = aggTrade.price.toDouble(),
                                    quantity = aggTrade.quantity.toDouble(),
                                    timestamp = aggTrade.tradeTime,
                                    isBuyerMaker = aggTrade.isBuyerMaker,
                                    side = if (aggTrade.isBuyerMaker) TradeSide.SELL else TradeSide.BUY
                                )

                                trySend(trade)

                            } catch (e: Exception) {
                                // Игнорируем ошибки парсинга
                            }
                        }
                        else -> {}
                    }
                }
            }
        } catch (e: Exception) {
            println("❌ Trades WebSocket failed: ${e.message}")
            throw e
        }

        close()
    }

    suspend fun getRecentTrades(symbol: String, limit: Int = 100): List<Trade> {
        // Можно добавить HTTP запрос для исторических сделок
        return emptyList()
    }
}