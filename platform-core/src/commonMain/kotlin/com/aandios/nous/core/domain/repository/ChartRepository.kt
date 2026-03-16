package com.aandios.nous.core.domain.repository

import com.aandios.nous.api.market.model.Candle
import kotlinx.coroutines.flow.Flow

interface ChartRepository {
    fun getChart(ticker: String, timeframe: String): Flow<List<Candle>>
}