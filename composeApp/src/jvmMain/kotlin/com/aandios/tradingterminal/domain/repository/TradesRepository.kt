package com.aandios.tradingterminal.domain.repository

import com.aandios.tradingterminal.data.api.binance.models.Trade
import kotlinx.coroutines.flow.Flow

interface TradesRepository {
    fun getTradesStream(symbol: String): Flow<Trade>
}