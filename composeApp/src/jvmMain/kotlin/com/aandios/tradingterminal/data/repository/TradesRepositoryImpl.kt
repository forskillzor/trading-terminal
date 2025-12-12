package com.aandios.tradingterminal.data.repository

import com.aandios.tradingterminal.data.api.binance.BinanceTradesApi
import com.aandios.tradingterminal.data.api.binance.models.Trade
import com.aandios.tradingterminal.domain.repository.TradesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow

class TradesRepositoryImpl(
    private val tradesApi: BinanceTradesApi
) : TradesRepository {

    private val activeSubscriptions = MutableStateFlow<Map<String, Flow<Trade>>>(emptyMap())

    override fun getTradesStream(symbol: String): Flow<Trade> {
        val key = symbol

        return activeSubscriptions.flatMapLatest { flows ->
            flows[key] ?: createTradesFlow(symbol).also { flow ->
                activeSubscriptions.value += (key to flow)
            }
        }
    }

    private fun createTradesFlow(symbol: String): Flow<Trade> = flow {
        println("📊 Trades Repository: Starting stream for $symbol")

        tradesApi.subscribeToAggTrades(symbol).collect { trade ->
            emit(trade)
        }
    }
}