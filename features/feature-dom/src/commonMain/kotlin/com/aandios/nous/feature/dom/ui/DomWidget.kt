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
import com.aandios.nous_platform.data.api.binance.models.BestPrices
import com.aandios.nous_platform.domain.commands.TradingCommand
import com.aandios.nous_platform.domain.entities.OrderBookData

@Composable
fun DomWidget(
    orderBook: OrderBookData?,
    bestPrices: BestPrices?,
    selectedPrice: Double?,
    onPriceSelected: (Double?) -> Unit,
    orderQuantity: String,
    onQuantityChanged: (String) -> Unit,
    onCreateBuyMarket: () -> TradingCommand,
    onCreateSellMarket: () -> TradingCommand,
    onCreateBuyLimit: (() -> TradingCommand?)?,
    onCreateSellLimit: (() -> TradingCommand?)?,
    onCreateBuyBestBid: (() -> TradingCommand?)?,
    onCreateSellBestAsk: (() -> TradingCommand?)?,
    onCreateTradeOff: () -> TradingCommand,
    onExecuteCommand: (TradingCommand?) -> Unit,
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
                bestPrices = bestPrices,
                selectedPrice = selectedPrice,
                onPriceSelected = onPriceSelected,
                orderQuantity = orderQuantity,
                onQuantityChanged = onQuantityChanged,
                onCreateBuyMarket = onCreateBuyMarket,
                onCreateSellMarket = onCreateSellMarket,
                onCreateBuyLimit = onCreateBuyLimit,
                onCreateSellLimit = onCreateSellLimit,
                onCreateBuyBestBid = onCreateBuyBestBid,
                onCreateSellBestAsk = onCreateSellBestAsk,
                onCreateTradeOff = onCreateTradeOff,
                onExecuteCommand = onExecuteCommand,
                isTradingEnabled = isTradingEnabled,
                modifier = Modifier.weight(1f)            )
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

