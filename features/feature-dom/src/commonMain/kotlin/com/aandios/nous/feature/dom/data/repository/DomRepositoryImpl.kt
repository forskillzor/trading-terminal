package com.aandios.nous.feature.dom.data.repository

import com.aandios.nous.api.market.adapters.DomAdapter
import com.aandios.nous.api.market.model.OrderBook
import com.aandios.nous.api.market.model.OrderBookLevel
import com.aandios.nous.core.domain.repository.DomRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class DomRepositoryImpl(
    private val domAdapter: DomAdapter
) : DomRepository {

    override fun getOrderBook(symbol: String): Flow<OrderBook> {
        return domAdapter.subscribeToOrderBook(symbol, depth = 20)
    }
}