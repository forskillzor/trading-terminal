package com.aandios.nous.api.market.adapters

import com.aandios.nous.api.market.model.Balance
import com.aandios.nous.api.market.model.OrderRequest
import com.aandios.nous.api.market.model.OrderResponse
import com.aandios.nous.api.market.model.Position


interface TradingAdapter: MarketAdapter {
    /**
     * Размещение ордера
     */
    suspend fun placeOrder(request: OrderRequest): OrderResponse

    /**
     * Отмена ордера
     */
    suspend fun cancelOrder(orderId: String): Boolean

    /**
     * Получение баланса
     */
    suspend fun getBalances(): List<Balance>

    /**
     * Получение открытых позиций
     */
    suspend fun getPositions(): List<Position>
}