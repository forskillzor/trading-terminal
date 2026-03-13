package com.aandios.nous.provider.binance

import com.aandios.nous.api.market.NetworkManager
import com.aandios.nous.api.market.Provider
import com.aandios.nous.api.market.ProviderConfig
import com.aandios.nous.api.market.adapters.*
import com.aandios.nous.api.market.model.Symbol
import com.aandios.nous.provider.binance.adapter.BinanceChartAdapter
import com.aandios.nous.provider.binance.adapter.BinanceDomAdapter
import com.aandios.nous.provider.binance.adapter.BinanceTradesAdapter
import com.aandios.nous.provider.binance.adapter.BinanceTradingAdapter

class BinanceProvider(
    private val networkManager: NetworkManager,
    private val config: ProviderConfig,
    private val tradesAdapter: BinanceTradesAdapter,
    private val domAdapter: BinanceDomAdapter,
    private val chartAdapter: BinanceChartAdapter,
    private val tradingAdapter: BinanceTradingAdapter
) : Provider {

    override val providerId = "binance"
    override val providerName = "Binance Exchange"
    override val version = "1.0.0"

    override val trades: TradesAdapter = tradesAdapter
    override val dom: DomAdapter = domAdapter
    override val chart: ChartAdapter = chartAdapter
    override val trading: TradingAdapter = tradingAdapter

    override suspend fun isAvailable(): Boolean {
        return try {
            // Простая проверка - пробуем получить символы
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