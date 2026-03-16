package com.aandios.nous.provider.binance

import com.aandios.nous.api.market.*
import com.aandios.nous.api.market.adapters.*
import com.aandios.nous.api.market.model.Symbol

class BinanceProvider(
    override val providerId: String,
    override val providerName: String,
    override val version: String,
    override val adapters: Map<AdapterType, MarketAdapter>
) : Provider {

    override suspend fun isAvailable(): Boolean {
        return try {
            getAvailableSymbols().isNotEmpty()
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun getAvailableSymbols(): List<Symbol> {
        // TODO: Реализовать запрос к Binance API
        return listOf(
            Symbol("BTCUSDT", "Bitcoin/USDT"),
            Symbol("ETHUSDT", "Ethereum/USDT")
        )
    }
}