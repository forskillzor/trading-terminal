package com.aandios.nous.provider.binance.adapter

import com.aandios.nous.api.market.ProviderConfig
import com.aandios.nous.api.market.adapters.BookTickerAdapter
import com.aandios.nous.api.market.adapters.DomAdapter
import com.aandios.nous.api.market.model.orderbook.DepthSnapshot
import com.aandios.nous.api.market.model.orderbook.DepthUpdate
import com.aandios.nous.provider.binance.BinanceProvider
import com.aandios.nous.provider.binance.model.BDepthUpdate
import com.aandios.nous.provider.binance.model.BinanceDepthSnapshot
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.websocket.*
import io.ktor.client.request.*
import io.ktor.websocket.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.serialization.json.Json

/**
 * Binance DOM Adapter
 *
 * Упрощенная архитектура:
 * - BestPrice (bookTicker) как источник правды для лучших цен
 * - Depth (depth) только для объемов на уровнях
 * - Хеш-таблица для хранения всех уровней (O(1) обновление)
 * - Фильтрация уровней по лучшим ценам при отдаче UI
 */
// todo maybe change to RPI Depth???
class BinanceDomAdapter(
    private val client: HttpClient,
    private val config: ProviderConfig,
    private val depthLimit: Int = 20,  // количество отображаемых уровней
    provider: BinanceProvider
) : DomAdapter {
    private val bookTickerAdapter: BookTickerAdapter = provider.bookTicker  // для получения BestPrice

    private val json = Json { ignoreUnknownKeys = true }

    /**
     * Состояние стакана - только хеш-таблицы цена -> объем
     */

    override suspend fun getOrderBookSnapshot(symbol: String, depth: Int): DepthSnapshot {
        // Получаем snapshot через REST
        val snapshot: BinanceDepthSnapshot = client.get("https://fapi.binance.com/fapi/v1/depth") {
            url {
                parameters.append("symbol", symbol)
                parameters.append("limit", depth.toString())  // максимум для снапшота
            }
        }.body()

        // Получаем текущие лучшие цены
        return snapshot.toDepthSnapshot()
    }


    /**
     * Подписка на depth обновления
     */
    override suspend fun subscribeToDepthUpdates(symbol: String): Flow<DepthUpdate> = callbackFlow {
        val streamName = "${symbol.lowercase()}@depth${depthLimit}@100ms"
        val endpoint = if (config.isTestnet) {
            "wss://testnet.binance.vision/ws/$streamName"
        } else {
            "wss://fstream.binance.com/ws/$streamName"
        }

        client.webSocket(urlString = endpoint) {
            println("✅ Depth WebSocket connected for $symbol")

            for (frame in incoming) {
                if (frame is Frame.Text) {
                    val update = json.decodeFromString<BDepthUpdate>(frame.readText())
                    trySend(update.toDepthUpdate())
                }
            }
        }

        close()
    }
}