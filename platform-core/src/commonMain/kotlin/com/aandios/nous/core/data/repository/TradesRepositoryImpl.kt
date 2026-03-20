package com.aandios.nous.core.data.repository

import com.aandios.nous.api.market.adapters.TradesAdapter
import com.aandios.nous.api.market.model.trades.Trade
import com.aandios.nous.core.domain.repository.TradesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow

class TradesRepositoryImpl(
    private val tradesAdapter: TradesAdapter
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

        tradesAdapter.subscribeToTrades(symbol).collect { trade ->
            emit(trade)
        }
    }
}