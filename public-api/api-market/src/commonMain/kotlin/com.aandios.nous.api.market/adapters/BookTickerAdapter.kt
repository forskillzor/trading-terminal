package com.aandios.nous.api.market.adapters

import com.aandios.nous.api.market.model.BookTicker
import kotlinx.coroutines.flow.Flow

interface BookTickerAdapter: MarketAdapter {
    fun subscribeToBookTicker(symbol: String): Flow<BookTicker>
}