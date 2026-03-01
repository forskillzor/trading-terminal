package com.aandios.tradingterminal.data.repository

import com.aandios.tradingterminal.data.api.binance.BinanceDomApi
import com.aandios.tradingterminal.domain.entities.OrderBookData
import com.aandios.tradingterminal.domain.entities.OrderBookLevel
import com.aandios.tradingterminal.domain.repository.DomRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow

class DomRepositoryImpl(
    private val domApi: BinanceDomApi
) : DomRepository {

    private val activeSubscriptions = MutableStateFlow<Map<String, Flow<OrderBookData>>>(emptyMap())

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun getOrderBook(symbol: String): Flow<OrderBookData> {

        return activeSubscriptions.flatMapLatest { flows ->
            flows[symbol] ?: createOrderBookFlow(symbol).also { flow ->
                activeSubscriptions.value += (symbol to flow)
            }
        }
    }

    private fun createOrderBookFlow(symbol: String): Flow<OrderBookData> = flow {
        domApi.subscribeToOrderBook(symbol).collect { orderBook ->
            emit(
                OrderBookData(
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

