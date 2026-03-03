package com.aandios.nous_platform.domain.repository

import com.aandios.nous_platform.data.api.binance.models.BestPrices
import kotlinx.coroutines.flow.Flow

interface BestPricesRepository {
    fun getBestPrices(symbol: String): Flow<BestPrices>
}
