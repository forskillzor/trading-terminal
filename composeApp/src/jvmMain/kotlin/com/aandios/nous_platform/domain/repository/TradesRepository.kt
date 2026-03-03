package com.aandios.nous_platform.domain.repository

import com.aandios.nous_platform.data.api.binance.models.Trade
import kotlinx.coroutines.flow.Flow

interface TradesRepository {
    fun getTradesStream(symbol: String): Flow<Trade>
}