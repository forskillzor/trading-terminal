package com.aandios.nous.feature.dom.ui.unified

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aandios.nous.api.market.commands.CommandResult
import com.aandios.nous.api.market.commands.TradingCommand
import com.aandios.nous.api.market.model.orderbook.OrderBook
import com.aandios.nous.feature.dom.domain.model.AggregationLevel
import com.aandios.nous.feature.dom.domain.UnifiedOrderBook
import com.aandios.nous.feature.dom.ui.OrderPlacementPanel

@Composable
fun UnifiedDomContent(
    aggregationLevel: AggregationLevel = AggregationLevel.TICK_0_1,
    unifiedOrderBook: UnifiedOrderBook,
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
        UnifiedDomSection(
            aggregationLevel = aggregationLevel,
            unifiedOrderBook = unifiedOrderBook,
            selectedPrice = selectedPrice,
            onPriceSelected = { price -> onPriceSelected(price) },
            modifier = Modifier.weight(1f)
        )

        OrderPlacementPanel(
            orderBook = OrderBook(
                symbol = unifiedOrderBook.symbol,
                bids = emptyList(), // не используются в панели
                asks = emptyList(),
                lastUpdateId = 0,
                timestamp = unifiedOrderBook.timestamp
            ),
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