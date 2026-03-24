package com.aandios.nous.feature.dom.ui.ninja

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
import com.aandios.nous.api.market.model.orderbook.OrderBook
import com.aandios.nous.feature.dom.domain.AggregationLevel
import com.aandios.nous.feature.dom.domain.UnifiedOrderBook
import com.aandios.nous.feature.dom.ui.OrderPlacementPanel
import com.aandios.nous.feature.dom.ui.ninja.DomNinjaTrader
import com.aandios.nous.feature.dom.ui.ninja.NinjaTraderDomUnified

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
        DomNinjaTrader(
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

/**
 * Версия DomContentNinja, которая использует UnifiedOrderBook вместо раздельных OrderBook и BookTicker.
 * Бизнес-логика преобразования уже выполнена в репозитории, UI только отображает готовые данные.
 */
@Composable
fun DomContentNinjaUnified(aggregationLevel: AggregationLevel = AggregationLevel.TICK_0_1, 
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
        // NinjaTrader стиль DOM с унифицированными данными
        NinjaTraderDomUnified(aggregationLevel = aggregationLevel, 
            unifiedOrderBook = unifiedOrderBook,
            selectedPrice = selectedPrice,
            onPriceSelected = { price -> onPriceSelected(price) },
            modifier = Modifier.weight(1f)
        )

        // Панель ордера с командами (используем OrderBook из unifiedOrderBook если нужно)
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