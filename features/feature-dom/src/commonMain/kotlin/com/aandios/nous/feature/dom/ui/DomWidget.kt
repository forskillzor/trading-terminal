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
import com.aandios.nous.feature.dom.domain.UnifiedOrderBook

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
    unifiedOrderBook: UnifiedOrderBook? = null) {
    Column(
        modifier = modifier
            .width(width)
            .fillMaxHeight()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        if (showHeader) {
            DomHeader(
                symbol = unifiedOrderBook?.symbol ?: orderBook?.symbol ?: "",
                timestamp = unifiedOrderBook?.timestamp ?: orderBook?.timestamp ?: 0
            )
        }

        val dataAvailable = unifiedOrderBook != null || orderBook != null
        if (dataAvailable) {
            if (unifiedOrderBook != null) {
                // Используем унифицированные данные (бизнес-логика уже в репозитории)
                DomContentNinjaUnified(
                    unifiedOrderBook = unifiedOrderBook,
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

