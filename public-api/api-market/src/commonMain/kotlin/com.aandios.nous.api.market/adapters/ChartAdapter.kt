package com.aandios.nous.api.market.adapters


import com.aandios.nous.api.market.model.Candle
import kotlinx.coroutines.flow.Flow

interface ChartAdapter {
    /**
     * Historical candles
     */
    suspend fun getCandles(
        symbol: String,
        interval: String,
        limit: Int = 500
    ): List<Candle>

    /**
     * Subscribe to realtime candles
     */
    fun subscribeToCandles(symbol: String, interval: String): Flow<Candle>
}