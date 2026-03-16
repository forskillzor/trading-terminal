package com.aandios.nous.core.data.repository

import com.aandios.nous.api.market.adapters.DomAdapter
import com.aandios.nous.api.market.model.OrderBook
import com.aandios.nous.api.market.model.OrderBookLevel
import com.aandios.nous.core.domain.repository.DomRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow

class DomRepositoryImpl(
    private val domAdapter: DomAdapter,
) : DomRepository {

    private val activeSubscriptions = MutableStateFlow<Map<String, Flow<OrderBook>>>(emptyMap())

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun getOrderBook(symbol: String): Flow<OrderBook> {

        return activeSubscriptions.flatMapLatest { flows ->
            flows[symbol] ?: createOrderBookFlow(symbol).also { flow ->
                activeSubscriptions.value += (symbol to flow)
            }
        }
    }

    private fun createOrderBookFlow(symbol: String): Flow<OrderBook> = flow {
        domAdapter.subscribeToOrderBook(symbol).collect { orderBook ->
            emit(
                OrderBook(
                    symbol = orderBook.symbol,
                    bids = orderBook.bids.map {
                        OrderBookLevel(
                            price = it.price,
                            quantity = it.quantity,
                            total = it.total
                        )
                    },
                    asks = orderBook.asks.map {
                        OrderBookLevel(
                            price = it.price,
                            quantity = it.quantity,
                            total = it.total
                        )
                    },
                    timestamp = orderBook.timestamp
                )
            )
        }
    }
}

