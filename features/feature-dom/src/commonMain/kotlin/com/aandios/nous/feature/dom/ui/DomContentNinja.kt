package com.aandios.nous.feature.dom.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aandios.nous.api.market.commands.CommandResult
import com.aandios.nous.api.market.commands.TradingCommand
import com.aandios.nous.api.market.model.BookTicker
import com.aandios.nous.api.market.model.OrderBook

@Composable
fun DomContentNinja(
    orderBook: OrderBook,
    bookTicker: BookTicker?,
    selectedPrice: Double?,
    onPriceSelected: (Double?) -> Unit,
    orderQuantity: String,
    onQuantityChanged: (String) -> Unit,
    onTradingCommand: (TradingCommand) -> Unit,
    onCommandResult: (CommandResult) -> Unit,
    isTradingEnabled: Boolean,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize()) {
        // NinjaTrader стиль DOM
        NinjaTraderDom(
            bids = orderBook.bids,
            asks = orderBook.asks,
            bookTicker = bookTicker,
            selectedPrice = selectedPrice,
            onPriceSelected = { price -> onPriceSelected(price) },
            modifier = Modifier.weight(1f)
        )

        // Панель ордера с командами
        OrderPlacementPanel(
            orderBook = orderBook,
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