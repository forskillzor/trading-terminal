package com.aandios.nous.api.market.adapters


import com.aandios.nous.api.market.model.Candle
import kotlinx.coroutines.flow.Flow

interface ChartAdapter: MarketAdapter {
    /**
     * Historical candles (most recent first)
     */
    suspend fun getCandles(
        symbol: String,
        interval: String,
        limit: Int = 500
    ): List<Candle>

    /**
     * Load historical candles BEFORE a given endTime (for infinite scroll backwards).
     * Returns candles sorted from oldest to newest.
     */
    suspend fun getCandlesBefore(
        symbol: String,
        interval: String,
        endTime: Long,
        limit: Int = 500
    ): List<Candle> = getCandles(symbol, interval, limit)

    /**
     * Subscribe to realtime candles
     */
    fun subscribeToCandles(symbol: String, interval: String): Flow<Candle>
}