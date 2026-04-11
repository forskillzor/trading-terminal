package com.aandios.nous.api.market.adapters

import com.aandios.nous.api.market.model.SymbolInfo

interface SymbolInfoAdapter: MarketAdapter {
    /**
     * Получить информацию о символе по его тикеру.
     */
    suspend fun getSymbolInfo(symbol: String): SymbolInfo?

    /**
     * Получить информацию о всех символах (опционально).
     */
    suspend fun getAllSymbolsInfo(): List<SymbolInfo> = emptyList()
}