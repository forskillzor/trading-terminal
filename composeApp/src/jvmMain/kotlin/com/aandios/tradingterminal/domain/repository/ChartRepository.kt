package com.aandios.tradingterminal.domain.repository

import com.aandios.tradingterminal.domain.entities.Candle
import kotlinx.coroutines.flow.Flow

interface ChartRepository {

    fun getChart(ticker: String, timeframe: String): Flow<List<Candle>>
}