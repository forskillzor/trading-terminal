package com.aandios.tradingterminal.data.repository

import com.aandios.tradingterminal.data.api.BinanceApi
import com.aandios.tradingterminal.data.api.BybitApi
import com.aandios.tradingterminal.domain.entities.Candle
import com.aandios.tradingterminal.domain.repository.ChartRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class ChartRepositoryImpl(
    private val bybitApi: BybitApi,
    private val binanceApi: BinanceApi
): ChartRepository {
    override fun getChart(
        ticker: String,
        timeframe: String
    ): Flow<List<Candle>> = flow {
        try {
            // Пока используем только Binance
            val binanceCandles = binanceApi.getCandles(
                symbol = ticker.replace("/", ""), // BTC/USDT -> BTCUSDT
                interval = mapTimeframe(timeframe),
                limit = 100
            )

            val domainCandles = binanceCandles.map { it.toDomain() }
            emit(domainCandles)
        } catch (e: Exception) {
            // В случае ошибки пробуем Bybit
            // TODO: реализовать Bybit
            throw e
        }
    }

    private fun mapTimeframe(timeframe: String): String {
        return when (timeframe) {
            "1m" -> "1m"
            "5m" -> "5m"
            "15m" -> "15m"
            "1h" -> "1h"
            "4h" -> "4h"
            "1d" -> "1d"
            else -> "1h"
        }
    }
}