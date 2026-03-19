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
import com.aandios.nous.api.market.model.OrderBook

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
    showHeader: Boolean = true) {
    Column(
        modifier = modifier
            .width(width)
            .fillMaxHeight()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        if (showHeader) {
            DomHeader(
                symbol = orderBook?.symbol ?: "",
                timestamp = orderBook?.timestamp ?: 0
            )
        }

        if (orderBook != null) {
            DomContentNinja(
                orderBook = orderBook,
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

