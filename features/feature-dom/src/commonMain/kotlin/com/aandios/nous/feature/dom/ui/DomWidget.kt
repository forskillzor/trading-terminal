package com.aandios.nous.feature.dom.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
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
import com.aandios.nous.feature.dom.ui.classic.DomContentSplit
import com.aandios.nous.feature.dom.ui.ninja.DomContentUnified

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
            // Вычисляем агрегированные цены bookticker для корректной подсветки при агрегации
            val aggregatedBookTicker = remember(bookTicker, aggregationLevel) {
                if (bookTicker == null) return@remember null
                // Для уровня TICK_0_1 используем точные цены, для других - округляем
                if (aggregationLevel == AggregationLevel.TICK_0_1) {
                    bookTicker
                } else {
                    // Создаем копию bookticker с округленными ценами
                    bookTicker.copy(
                        bestBid = aggregationLevel.roundDown(bookTicker.bestBid),
                        bestAsk = aggregationLevel.roundDown(bookTicker.bestAsk)
                    )
                }
            }

            when (domMode) {
                DomMode.CLASSIC -> {
                    // Классический DOM с раздельными bid/ask потоками
                    DomContentSplit(
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
                    // todo Здесь надо получать реальный размер тика из SymbolInfoAdapter
//                    val displayOrderBook = if (aggregationLevel != AggregationLevel.TICK_0_1 && unifiedOrderBook != null) {
//                        unifiedOrderBook.aggregate(aggregationLevel)
//                    } else {
//                        unifiedOrderBook
//                    }

                    // Используем унифицированные данные (бизнес-логика уже в репозитории)
                    DomContentUnified(
                        unifiedOrderBook = unifiedOrderBook!!.aggregate(aggregationLevel),
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

