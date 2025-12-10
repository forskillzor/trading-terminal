package com.aandios.tradingterminal.domain.usecases

import com.aandios.tradingterminal.domain.entities.Candle
import com.aandios.tradingterminal.domain.repository.ChartRepository
import kotlinx.coroutines.flow.Flow

interface GetChartByTickerUseCase {
    operator fun invoke(ticker: String, timeframe: String ): Flow<List<Candle>>
}

class GetChartByTickerUseCaseImpl(
    private val repository: ChartRepository
): GetChartByTickerUseCase {
    override fun invoke(ticker: String, timeframe: String): Flow<List<Candle>> {
        return repository.getChart(ticker, timeframe)
    }
}