package com.aandios.nous.provider.binance

import com.aandios.nous.api.market.NetworkManager
import com.aandios.nous.api.market.Provider
import com.aandios.nous.api.market.ProviderConfig
import com.aandios.nous.api.market.adapters.*
import com.aandios.nous.provider.binance.adapter.BinanceBookTickerAdapter
import com.aandios.nous.provider.binance.adapter.BinanceChartAdapter
import com.aandios.nous.provider.binance.adapter.BinanceDomAdapter
import com.aandios.nous.provider.binance.adapter.BinanceSymbolInfoAdapter
import com.aandios.nous.provider.binance.adapter.BinanceTradesAdapter
import com.aandios.nous.provider.binance.adapter.BinanceTradingAdapter

class BinanceProvider(
    override val providerId: String,
    override val providerName: String,
    override val version: String,
    override val config: ProviderConfig,
    override val networkManager: NetworkManager,
) : Provider {
    override val trades by lazy { BinanceTradesAdapter(networkManager.httpClient, config) }
    override val dom: DomAdapter by lazy { BinanceDomAdapter(networkManager.httpClient, config, provider = this) }
    override val bookTicker: BookTickerAdapter by lazy { BinanceBookTickerAdapter(networkManager.httpClient, config) }
    override val chart: ChartAdapter by lazy { BinanceChartAdapter(networkManager.httpClient, config) }
    override val trading: TradingAdapter by lazy { BinanceTradingAdapter(networkManager.httpClient, config) }
    override val symbolInfo: SymbolInfoAdapter by lazy { BinanceSymbolInfoAdapter(networkManager.httpClient, config) }

}