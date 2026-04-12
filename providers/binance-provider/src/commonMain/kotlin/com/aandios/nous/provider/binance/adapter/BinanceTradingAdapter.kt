package com.aandios.nous.provider.binance.adapter

import com.aandios.nous.api.market.ProviderConfig
import com.aandios.nous.api.market.adapters.TradingAdapter
import com.aandios.nous.api.market.model.Balance
import com.aandios.nous.api.market.model.trading.OrderRequest
import com.aandios.nous.api.market.model.trading.OrderResponse
import com.aandios.nous.api.market.model.trading.Position
import io.ktor.client.*

class BinanceTradingAdapter(
    client: HttpClient,
    val config: ProviderConfig
): TradingAdapter {
    override suspend fun placeOrder(request: OrderRequest): OrderResponse {
        println("📝 BinanceTradingAdapter.placeOrder: $request")
        // Заглушка: всегда успех
        // fixme: Реальная реализация должна вызывать Binance API
        return OrderResponse(
            orderId = "TEST-${System.currentTimeMillis()}",
            price = request.price
        )
    }

    override suspend fun cancelOrder(orderId: String): Boolean {
        println("📝 BinanceTradingAdapter.cancelOrder: $orderId")
        // Заглушка: всегда успех
        return true
    }

    override suspend fun getBalances(): List<Balance> {
        println("📝 BinanceTradingAdapter.getBalances")
        // Заглушка: пустой список
        return emptyList()
    }

    override suspend fun getPositions(): List<Position> {
        println("📝 BinanceTradingAdapter.getPositions")
        // Заглушка: пустой список
        return emptyList()
    }
}