package com.aandios.nous.provider.binance.adapter

import com.aandios.nous.api.market.ProviderConfig
import com.aandios.nous.api.market.adapters.TradingAdapter
import com.aandios.nous.api.market.model.Balance
import com.aandios.nous.api.market.model.OrderRequest
import com.aandios.nous.api.market.model.OrderResponse
import com.aandios.nous.api.market.model.Position
import io.ktor.client.*

class BinanceTradingAdapter(
    client: HttpClient,
    val config: ProviderConfig
): TradingAdapter {
    override suspend fun placeOrder(request: OrderRequest): OrderResponse {
        TODO("Not yet implemented")
    }

    override suspend fun cancelOrder(orderId: String): Boolean {
        TODO("Not yet implemented")
    }

    override suspend fun getBalances(): List<Balance> {
        TODO("Not yet implemented")
    }

    override suspend fun getPositions(): List<Position> {
        TODO("Not yet implemented")
    }
}