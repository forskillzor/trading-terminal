package com.aandios.nous.core.domain.repository

import com.aandios.nous.api.market.model.SymbolInfo

interface SymbolInfoRepository {
    /**
     * Получить информацию о символе по его тикеру.
     * Возвращает null, если информация недоступна.
     */
    suspend fun getSymbolInfo(symbol: String): SymbolInfo?

    /**
     * Получить информацию о всех символах (опционально).
     */
    suspend fun getAllSymbolsInfo(): List<SymbolInfo> = emptyList()
}