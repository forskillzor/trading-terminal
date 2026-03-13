package com.aandios.nous.core.domain.repository

import com.aandios.nous.core.domain.entities.dom.BestPrices
import kotlinx.coroutines.flow.Flow

interface BestPricesRepository {
    fun getBestPrices(symbol: String): Flow<BestPrices>
}