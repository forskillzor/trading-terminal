package com.aandios.tradingterminal.domain.usecases

import com.aandios.tradingterminal.domain.entities.Candle
import kotlinx.coroutines.flow.Flow

interface ObserveRealTimeChartUseCase {
    operator fun invoke(ticker: String, timeframe: String): Flow<Candle>
}