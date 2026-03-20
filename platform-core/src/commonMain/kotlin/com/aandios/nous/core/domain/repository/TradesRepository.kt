package com.aandios.nous.core.domain.repository

import com.aandios.nous.api.market.model.trades.Trade
import kotlinx.coroutines.flow.Flow

interface TradesRepository {
    fun getTradesStream(symbol: String): Flow<Trade>
}