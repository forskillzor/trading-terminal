package com.aandios.nous_platform.ui.dom

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aandios.nous_platform.data.api.binance.models.BestPrices
import com.aandios.nous_platform.domain.commands.TradingCommand
import com.aandios.nous_platform.domain.entities.OrderBookData

@Composable
fun DomContentNinja(
    orderBook: OrderBookData,
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
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize()) {
        // NinjaTrader стиль DOM
        NinjaTraderDom(
            bids = orderBook.bids,
            asks = orderBook.asks,
            bestPrices = bestPrices,
            selectedPrice = selectedPrice,
            onPriceSelected = { price -> onPriceSelected(price) },
            modifier = Modifier.weight(1f)
        )

        // Панель ордера с командами
        OrderPlacementPanel(
            selectedPrice = selectedPrice,
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
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)  // Высота под все кнопки
        )
    }
}