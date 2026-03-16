package com.aandios.nous_platform.data.repository

import com.aandios.nous_platform.data.api.binance.BinanceBookTickerApi
import com.aandios.nous_platform.data.api.binance.models.BestPrices
import com.aandios.nous_platform.domain.repository.BookTickerRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class BookTickerRepositoryImpl(
    private val bestPricesApi: BinanceBookTickerApi
) : BookTickerRepository {

    override fun getBookTicker(symbol: String): Flow<BestPrices> {
        return bestPricesApi.subscribeToBookTicker(symbol)
            .map { ticker -> ticker.toDomain() }
    }
}