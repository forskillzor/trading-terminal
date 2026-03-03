package com.aandios.nous_platform.domain.usecases

import com.aandios.nous_platform.domain.entities.Candle
import kotlinx.coroutines.flow.Flow

interface ObserveRealTimeChartUseCase {
    operator fun invoke(ticker: String, timeframe: String): Flow<Candle>
}