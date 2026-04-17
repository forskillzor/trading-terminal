package com.aandios.nous.feature.dom.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aandios.nous.api.market.commands.CommandResult
import com.aandios.nous.api.market.commands.TradingCommand
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
    symbolTickSize: Double? = null,
    selectedPrice: Double? = null,
    onPriceSelected: (Double?) -> Unit = {},
    // Order placement parameters
    orderQuantity: String = "0.01",
    onQuantityChanged: (String) -> Unit = {},
    onTradingCommand: (TradingCommand) -> Unit = {},
    onCommandResult: (CommandResult) -> Unit = {},
    isTradingEnabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize()) {
        // DOM content area (takes remaining space)
        Box(modifier = Modifier.weight(1f)) {
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
                                splitViewMode = domOptions.splitViewMode,
                                baseTickSize = symbolTickSize,
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
                            // Применяем агрегацию к данным, если уровень агрегации не равен BaseTick и известен symbolTickSize
                            val displayUnifiedOrderBook = if (domOptions.aggregation != AggregationLevel.BaseTick && symbolTickSize != null) {
                                baseUnifiedOrderBook.aggregate(domOptions.aggregation, symbolTickSize)
                            } else {
                                baseUnifiedOrderBook
                            }
                            
                            UnifiedDomContent(
                                unifiedOrderBook = displayUnifiedOrderBook,
                                aggregationLevel = domOptions.aggregation,
                                baseTickSize = symbolTickSize,
                                selectedPrice = selectedPrice,
                                onPriceSelected = onPriceSelected,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }
            }
        }
        
        // Order Placement Panel (fixed height)
        val panelOrderBook = orderBook ?: unifiedOrderBook?.toOrderBook()
        if (panelOrderBook != null) {
            OrderPlacementPanel(
                orderBook = panelOrderBook,
                selectedPrice = selectedPrice,
                orderQuantity = orderQuantity,
                onQuantityChanged = onQuantityChanged,
                onTradingCommand = onTradingCommand,
                onCommandResult = onCommandResult,
                isTradingEnabled = isTradingEnabled,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
            )
        }
    }
}