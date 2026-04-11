package com.aandios.nous.feature.dom.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aandios.nous.api.market.model.BookTicker
import com.aandios.nous.api.market.model.orderbook.OrderBook
import com.aandios.nous.feature.dom.domain.DomMode
import com.aandios.nous.feature.dom.domain.DomOptions
import com.aandios.nous.feature.dom.domain.UnifiedOrderBook
import com.aandios.nous.feature.dom.domain.model.AggregationLevel
import com.aandios.nous.feature.dom.ui.split.SplitDomContent
import com.aandios.nous.feature.dom.ui.unified.UnifiedDomContent

@Composable
fun DomContent(
    orderBook: OrderBook?,
    unifiedOrderBook: UnifiedOrderBook?,
    bookTicker: BookTicker?,
    domOptions: DomOptions,
    selectedPrice: Double? = null,
    onPriceSelected: (Double?) -> Unit = {},
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
        val dataAvailable = unifiedOrderBook != null || orderBook != null
        
        if (!dataAvailable) {
            CircularProgressIndicator(
                modifier = Modifier
                    .size(24.dp)
                    .align(Alignment.Center),
                strokeWidth = 2.dp
            )
        } else {
            when (domOptions.mode) {
                DomMode.SPLIT -> {
                    if (orderBook == null) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .size(24.dp)
                                .align(Alignment.Center),
                            strokeWidth = 2.dp
                        )
                    } else {
                        SplitDomContent(
                            orderBook = orderBook,
                            bookTicker = bookTicker,
                            selectedPrice = selectedPrice,
                            onPriceSelected = onPriceSelected,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
                
                DomMode.UNIFIED -> {
                    // Создаем UnifiedOrderBook, если его нет
                    val baseUnifiedOrderBook = unifiedOrderBook ?: orderBook?.let { 
                        UnifiedOrderBook.fromOrderBook(it, bookTicker) 
                    }
                    
                    if (baseUnifiedOrderBook == null) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .size(24.dp)
                                .align(Alignment.Center),
                            strokeWidth = 2.dp
                        )
                    } else {
                        // Применяем агрегацию к данным, если уровень агрегации не равен TICK_0_1
                        val displayUnifiedOrderBook = if (domOptions.aggregation != AggregationLevel.TICK_0_1) {
                            baseUnifiedOrderBook.aggregate(domOptions.aggregation)
                        } else {
                            baseUnifiedOrderBook
                        }
                        
                        UnifiedDomContent(
                            unifiedOrderBook = displayUnifiedOrderBook,
                            aggregationLevel = domOptions.aggregation,
                            selectedPrice = selectedPrice,
                            onPriceSelected = onPriceSelected,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        }
    }
}