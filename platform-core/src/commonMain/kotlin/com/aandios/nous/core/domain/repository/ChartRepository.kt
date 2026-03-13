package com.aandios.nous.core.domain.repository

import com.aandios.nous.core.domain.entities.chart.Candle
import kotlinx.coroutines.flow.Flow

interface ChartRepository {
    fun getChart(ticker: String, timeframe: String): Flow<List<Candle>>
}