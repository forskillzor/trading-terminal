package com.aandios.nous_platform.domain.usecases

import com.aandios.nous_platform.domain.entities.Candle
import com.aandios.nous_platform.domain.repository.ChartRepository
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