package com.aandios.nous.feature.dom.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.aandios.nous.api.market.commands.CommandResult
import com.aandios.nous.api.market.commands.TradingCommand
import com.aandios.nous.api.market.model.BookTicker
import com.aandios.nous.api.market.model.orderbook.OrderBook
import com.aandios.nous.feature.dom.domain.AggregationLevel
import com.aandios.nous.feature.dom.domain.UnifiedOrderBook
import com.aandios.nous.feature.dom.ui.classic.DomContentClassic
import com.aandios.nous.feature.dom.ui.ninja.DomContentNinja
import com.aandios.nous.feature.dom.ui.ninja.DomContentNinjaUnified

enum class DomMode {
    CLASSIC,
    NINJA
}

@Composable
fun DomWidget(
    orderBook: OrderBook?,
    bookTicker: BookTicker?,
    selectedPrice: Double?,
    onPriceSelected: (Double?) -> Unit,
    orderQuantity: String,
    onQuantityChanged: (String) -> Unit,
    onTradingCommand: (TradingCommand) -> Unit,
    onCommandResult: (CommandResult) -> Unit,
    isTradingEnabled: Boolean,
    modifier: Modifier = Modifier,
    width: Dp = 300.dp,
    showHeader: Boolean = true,
    unifiedOrderBook: UnifiedOrderBook? = null,
    domMode: DomMode = DomMode.NINJA,
    onDomModeChanged: (DomMode) -> Unit = {},
    aggregationLevel: AggregationLevel = AggregationLevel.TICK_0_1,
    onAggregationLevelChanged: (AggregationLevel) -> Unit = {}) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .widthIn(min = width)
            .background(MaterialTheme.colorScheme.surface)
    ) {
        if (showHeader) {
            DomHeader(
                symbol = unifiedOrderBook?.symbol ?: orderBook?.symbol ?: "",
                timestamp = unifiedOrderBook?.timestamp ?: orderBook?.timestamp ?: 0,
                currentMode = domMode,
                onModeChanged = onDomModeChanged,
                aggregationLevel = aggregationLevel,
                onAggregationLevelChanged = onAggregationLevelChanged
            )
        }

        val dataAvailable = unifiedOrderBook != null || orderBook != null
        if (dataAvailable) {
            when (domMode) {
                DomMode.CLASSIC -> {
                    // Классический DOM с раздельными bid/ask потоками
                    DomContentClassic(
                        orderBook = orderBook!!,
                        selectedPrice = selectedPrice,
                        onPriceSelected = onPriceSelected,
                        orderQuantity = orderQuantity,
                        onQuantityChanged = onQuantityChanged,
                        onTradingCommand = onTradingCommand,
                        onCommandResult = onCommandResult,
                        isTradingEnabled = isTradingEnabled,
                        modifier = Modifier.weight(1f),
                    )
                }
                DomMode.NINJA -> {
                    // Применяем агрегацию к данным, если уровень агрегации не равен TICK_0_1
                    val displayOrderBook = if (aggregationLevel != AggregationLevel.TICK_0_1 && unifiedOrderBook != null) {
                        unifiedOrderBook.aggregate(aggregationLevel)
                    } else {
                        unifiedOrderBook
                    }
                    
                    if (displayOrderBook != null) {
                        // Используем унифицированные данные (бизнес-логика уже в репозитории)
                        DomContentNinjaUnified(
                            unifiedOrderBook = displayOrderBook,
                            selectedPrice = selectedPrice,
                            onPriceSelected = onPriceSelected,
                            orderQuantity = orderQuantity,
                            onQuantityChanged = onQuantityChanged,
                            onTradingCommand = onTradingCommand,
                            onCommandResult = onCommandResult,
                            isTradingEnabled = isTradingEnabled,
                            modifier = Modifier.weight(1f),
                        )
                    } else {
                        // Fallback к старой логике (для обратной совместимости)
                        DomContentNinja(
                            orderBook = orderBook!!,
                            bookTicker = bookTicker,
                            selectedPrice = selectedPrice,
                            onPriceSelected = onPriceSelected,
                            orderQuantity = orderQuantity,
                            onQuantityChanged = onQuantityChanged,
                            onTradingCommand = onTradingCommand,
                            onCommandResult = onCommandResult,
                            isTradingEnabled = isTradingEnabled,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        } else {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.dp
                )
            }
        }
    }
}

