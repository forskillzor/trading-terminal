package com.aandios.nous.feature.dom.domain

import com.aandios.nous.api.market.model.SymbolInfo

/**
 * Торговый символ (пара) для отображения в DOM.
 */
data class TradingSymbol(
    val symbol: String,
    val displayName: String,
    val provider: TradingProvider
) {
    companion object {

        /**
         * Создаёт TradingSymbol из SymbolInfo (данные symbolInfoAdapter).
         * Форматирует displayName через baseAsset/quoteAsset, если доступны.
         */
        fun fromSymbolInfo(info: SymbolInfo, provider: TradingProvider): TradingSymbol {
            val displayName = if (info.baseAsset.isNotEmpty() && info.quoteAsset.isNotEmpty()) {
                "${info.baseAsset}/${info.quoteAsset}"
            } else {
                info.symbol
            }
            return TradingSymbol(
                symbol = info.symbol,
                displayName = displayName,
                provider = provider
            )
        }
        /**
         * Стандартные символы для Binance Spot.
         */
        val BINANCE_SPOT_SYMBOLS = listOf(
            TradingSymbol("BTCUSDT", "BTC/USDT", TradingProvider.BINANCE),
            TradingSymbol("ETHUSDT", "ETH/USDT", TradingProvider.BINANCE),
            TradingSymbol("BNBUSDT", "BNB/USDT", TradingProvider.BINANCE),
            TradingSymbol("SOLUSDT", "SOL/USDT", TradingProvider.BINANCE),
            TradingSymbol("XRPUSDT", "XRP/USDT", TradingProvider.BINANCE),
            TradingSymbol("ADAUSDT", "ADA/USDT", TradingProvider.BINANCE),
            TradingSymbol("DOGEUSDT", "DOGE/USDT", TradingProvider.BINANCE),
            TradingSymbol("DOTUSDT", "DOT/USDT", TradingProvider.BINANCE),
            TradingSymbol("AVAXUSDT", "AVAX/USDT", TradingProvider.BINANCE),
            TradingSymbol("LINKUSDT", "LINK/USDT", TradingProvider.BINANCE),
        )

        /**
         * Стандартные символы для Binance Coin-M Futures.
         */
        val BINANCE_COIN_M_FUTURES_SYMBOLS = listOf(
            TradingSymbol("BTCUSD_PERP", "BTCUSD Perp", TradingProvider.BINANCE_COIN_M),
            TradingSymbol("ETHUSD_PERP", "ETHUSD Perp", TradingProvider.BINANCE_COIN_M),
            TradingSymbol("BNBUSD_PERP", "BNBUSD Perp", TradingProvider.BINANCE_COIN_M),
            TradingSymbol("SOLUSD_PERP", "SOLUSD Perp", TradingProvider.BINANCE_COIN_M),
            TradingSymbol("XRPUSD_PERP", "XRPUSD Perp", TradingProvider.BINANCE_COIN_M),
            TradingSymbol("ADAUSD_PERP", "ADAUSD Perp", TradingProvider.BINANCE_COIN_M),
            TradingSymbol("DOGEUSD_PERP", "DOGEUSD Perp", TradingProvider.BINANCE_COIN_M),
        )

        /**
         * Стандартные символы для Bybit Spot.
         */
        val BYBIT_SPOT_SYMBOLS = listOf(
            TradingSymbol("BTCUSDT", "BTC/USDT", TradingProvider.BYBIT),
            TradingSymbol("ETHUSDT", "ETH/USDT", TradingProvider.BYBIT),
            TradingSymbol("XRPUSDT", "XRP/USDT", TradingProvider.BYBIT),
            TradingSymbol("SOLUSDT", "SOL/USDT", TradingProvider.BYBIT),
        )

        /**
         * Получает список символов для указанного провайдера.
         */
        fun getSymbolsForProvider(provider: TradingProvider): List<TradingSymbol> {
            return when (provider) {
                TradingProvider.BINANCE -> BINANCE_SPOT_SYMBOLS
                TradingProvider.BINANCE_COIN_M -> BINANCE_COIN_M_FUTURES_SYMBOLS
                TradingProvider.BINANCE_USDM -> BINANCE_SPOT_SYMBOLS.take(5) // Пример для USD-M
                TradingProvider.BYBIT -> BYBIT_SPOT_SYMBOLS
                TradingProvider.KRAKEN -> BINANCE_SPOT_SYMBOLS.take(5) // Пример для Kraken
            }
        }

        /**
         * Находит символ по строковому идентификатору.
         */
        fun findSymbol(symbolString: String, provider: TradingProvider): TradingSymbol? {
            return getSymbolsForProvider(provider).find { it.symbol == symbolString }
        }

        /**
         * Создает символ по умолчанию для провайдера.
         */
        fun defaultForProvider(provider: TradingProvider): TradingSymbol {
            return getSymbolsForProvider(provider).firstOrNull() ?: 
                TradingSymbol("BTCUSDT", "BTC/USDT", provider)
        }

        /**
         * Возвращает символ по умолчанию (для Binance Coin-M Futures).
         */
        fun default(): TradingSymbol = defaultForProvider(TradingProvider.BINANCE_COIN_M)
    }

    override fun toString(): String = displayName
}