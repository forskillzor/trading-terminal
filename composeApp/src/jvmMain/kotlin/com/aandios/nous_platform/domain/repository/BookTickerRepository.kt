package com.aandios.nous_platform.domain.repository

import com.aandios.nous_platform.data.api.binance.models.BestPrices
import kotlinx.coroutines.flow.Flow

interface BookTickerRepository {

    fun getBookTicker(symbol: String): Flow<BestPrices>
}
