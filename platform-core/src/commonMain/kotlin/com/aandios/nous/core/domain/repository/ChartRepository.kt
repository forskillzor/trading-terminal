package com.aandios.nous.core.domain.repository

import com.aandios.nous.api.market.model.Candle
import kotlinx.coroutines.flow.Flow

interface ChartRepository {
    fun getChart(ticker: String, timeframe: String): Flow<List<Candle>>

    /**
     * Load historical candles before a given time (for infinite scroll backwards).
     * Returns candles sorted from oldest to newest.
     */
    suspend fun loadHistoricalCandlesBefore(
        ticker: String,
        timeframe: String,
        endTime: Long,
        limit: Int = 200
    ): List<Candle>
}