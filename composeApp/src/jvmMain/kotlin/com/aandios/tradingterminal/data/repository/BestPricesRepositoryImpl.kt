package com.aandios.tradingterminal.data.repository

import com.aandios.tradingterminal.data.api.binance.BinanceBestPricesApi
import com.aandios.tradingterminal.data.api.binance.models.BestPrices
import com.aandios.tradingterminal.domain.repository.BestPricesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class BestPricesRepositoryImpl(
    private val bestPricesApi: BinanceBestPricesApi
) : BestPricesRepository {

    override fun getBestPrices(symbol: String): Flow<BestPrices> {
        return bestPricesApi.subscribeToBookTicker(symbol)  // ← меняем название метода
            .map { ticker -> ticker.toDomain() }
    }
}