package com.aandios.nous.core.domain.repository

import com.aandios.nous.api.market.model.BookTicker
import kotlinx.coroutines.flow.Flow

interface BookTickerRepository {

    fun getBookTicker(symbol: String): Flow<BookTicker>
}
