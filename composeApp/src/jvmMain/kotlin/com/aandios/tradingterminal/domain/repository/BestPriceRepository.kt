package com.aandios.tradingterminal.domain.repository

import com.aandios.tradingterminal.data.api.binance.models.BestPrices
import kotlinx.coroutines.flow.Flow

interface BestPricesRepository {
    fun getBestPrices(symbol: String): Flow<BestPrices>
}
