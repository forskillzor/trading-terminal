package com.aandios.nous.api.market

import com.aandios.nous.api.market.adapters.ChartAdapter
import com.aandios.nous.api.market.adapters.DomAdapter
import com.aandios.nous.api.market.adapters.TradesAdapter
import com.aandios.nous.api.market.adapters.TradingAdapter
import com.aandios.nous.api.market.model.Symbol

/**
 * Композитный провайдер - объединяет все адаптеры
 * Каждая биржа предоставляет свою реализацию
 */
interface Provider {
    val providerId: String
    val providerName: String
    val version: String

    val trades: TradesAdapter?
    val dom: DomAdapter?
    val chart: ChartAdapter?
    val trading: TradingAdapter?

    /**
     * Проверка доступности провайдера
     */
    suspend fun isAvailable(): Boolean

    /**
     * Получение списка поддерживаемых символов
     */
    suspend fun getAvailableSymbols(): List<Symbol>

    companion object {
        /**
         * Создание провайдера с выборочной поддержкой
         */
        fun of(
            id: String,
            name: String,
            version: String,
            trades: TradesAdapter? = null,
            dom: DomAdapter? = null,
            chart: ChartAdapter? = null,
            trading: TradingAdapter? = null
        ): Provider = object : Provider {
            override val providerId = id
            override val providerName = name
            override val version = version
            override val trades = trades
            override val dom = dom
            override val chart = chart
            override val trading = trading

            override suspend fun isAvailable(): Boolean = true
            override suspend fun getAvailableSymbols(): List<Symbol> = emptyList()
        }
    }
}