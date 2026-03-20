package com.aandios.nous.provider.binance.adapter

import com.aandios.nous.api.market.ProviderConfig
import com.aandios.nous.api.market.adapters.TradesAdapter
import com.aandios.nous.api.market.model.trades.Trade
import com.aandios.nous.provider.binance.model.BinanceAggTrade
import io.ktor.client.*
import io.ktor.client.plugins.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.serialization.json.Json

class BinanceTradesAdapter(
    private val httpClient: HttpClient,
    private val config: ProviderConfig
) : TradesAdapter {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }

    override fun subscribeToTrades(symbol: String): Flow<Trade> = callbackFlow {
        val streamName = "${symbol.lowercase()}@aggTrade"
        val endpoint = if (config.isTestnet) {
            "wss://testnet.binance.vision/ws/$streamName"
        } else {
            "wss://fstream.binance.com/ws/$streamName"
        }

        try {
            httpClient.webSocket(urlString = endpoint) {
                for (frame in incoming) {
                    if (frame is Frame.Text) {
                        val text = frame.readText()
                        try {
                            val aggTrade = json.decodeFromString<BinanceAggTrade>(text)
                            trySend(aggTrade.toTrade())
                        } catch (e: Exception) {
                            // Log error
                        }
                    }
                }
            }
        } catch (e: Exception) {
            println("❌ Trades WebSocket failed: ${e.message}")
            throw e
        }
        close()
    }

    override suspend fun getRecentTrades(symbol: String, limit: Int): List<Trade> {
        return emptyList() // Implement if needed
    }
}