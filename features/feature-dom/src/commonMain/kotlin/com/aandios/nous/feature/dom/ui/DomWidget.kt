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
import com.aandios.nous.feature.dom.domain.*
import com.aandios.nous.feature.dom.domain.model.AggregationLevel
import com.aandios.nous.feature.dom.domain.model.DepthLimit
import com.aandios.nous.feature.dom.ui.split.SplitDomContent
import com.aandios.nous.feature.dom.ui.unified.UnifiedDomContent

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
    onAggregationLevelChanged: (AggregationLevel) -> Unit = {},
    subscriptionDepth: SubscriptionDepth = SubscriptionDepth.default(),
    onSubscriptionDepthChanged: (SubscriptionDepth) -> Unit = {},
    tradingProvider: TradingProvider = TradingProvider.BINANCE,
    onTradingProviderChanged: (TradingProvider) -> Unit = {},
    tradingSymbol: TradingSymbol = TradingSymbol.default(),
    onTradingSymbolChanged: (TradingSymbol) -> Unit = {},
    depthLimit: DepthLimit = DepthLimit.default(),
    onDepthLimitChanged: (DepthLimit) -> Unit = {},
    collapsed: Boolean = false,
    onToggleCollapsed: () -> Unit = {},
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .widthIn(min = width)
            .background(MaterialTheme.colorScheme.surface)
    ) {
        if (showHeader) {
            DomHeader(
                tradingProvider = tradingProvider,
                onTradingProviderChanged = onTradingProviderChanged,
                tradingSymbol = tradingSymbol,
                onSymbolChanged = onTradingSymbolChanged,
                depthLimit = depthLimit,
                onDepthLimitChanged = onDepthLimitChanged,
                aggregationLevel = aggregationLevel,
                onAggregationLevelChanged = onAggregationLevelChanged,
                subscriptionDepth = subscriptionDepth,
                onSubscriptionDepthChanged = onSubscriptionDepthChanged,
                domMode = domMode,
                onDomModeChanged = onDomModeChanged,
                collapsed = collapsed,
                onToggleCollapsed = onToggleCollapsed
            )
        }

        val dataAvailable = unifiedOrderBook != null || orderBook != null
        if (dataAvailable) {


            when (domMode) {
                DomMode.CLASSIC -> {
                    if (orderBook == null) {
                        // Нет данных для отображения
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp
                            )
                        }
                    } else {
                        // Классический DOM с раздельными bid/ask потоками
                        SplitDomContent(
                            orderBook = orderBook,
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

                DomMode.NINJA -> {
                    // Создаем UnifiedOrderBook, если его нет
                    val baseUnifiedOrderBook = unifiedOrderBook ?: orderBook?.let { 
                        UnifiedOrderBook.fromOrderBook(it, bookTicker) 
                    }
                    
                    if (baseUnifiedOrderBook == null) {
                        // Нет данных для отображения
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp
                            )
                        }
                    } else {
                        // Применяем агрегацию к данным, если уровень агрегации не равен TICK_0_1
                        val displayUnifiedOrderBook = if (aggregationLevel != AggregationLevel.TICK_0_1) {
                            baseUnifiedOrderBook.aggregate(aggregationLevel)
                        } else {
                            baseUnifiedOrderBook
                        }

                        // Используем унифицированные данные
                        UnifiedDomContent(
                            unifiedOrderBook = displayUnifiedOrderBook,
                            selectedPrice = selectedPrice,
                            onPriceSelected = onPriceSelected,
                            orderQuantity = orderQuantity,
                            onQuantityChanged = onQuantityChanged,
                            onTradingCommand = onTradingCommand,
                            onCommandResult = onCommandResult,
                            isTradingEnabled = isTradingEnabled,
                            aggregationLevel = aggregationLevel,
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

