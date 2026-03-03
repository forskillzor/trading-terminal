package com.aandios.nous_platform.domain.repository

import com.aandios.nous_platform.domain.entities.Candle
import kotlinx.coroutines.flow.Flow

interface ChartRepository {

    fun getChart(ticker: String, timeframe: String): Flow<List<Candle>>
}