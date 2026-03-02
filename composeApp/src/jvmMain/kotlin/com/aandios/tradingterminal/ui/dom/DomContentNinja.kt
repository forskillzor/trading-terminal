package com.aandios.tradingterminal.ui.dom

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aandios.tradingterminal.domain.entities.OrderBookData
import com.aandios.tradingterminal.domain.entities.OrderSide

@Composable
fun DomContentNinja(
    orderBook: OrderBookData,
    selectedPrice: Double?,
    onPriceSelected: (Double?) -> Unit,
    orderQuantity: String,
    onQuantityChanged: (String) -> Unit,
    onPlaceOrder: (OrderSide) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize()) {
        // NinjaTrader стиль DOM
        NinjaTraderDom(
            bids = orderBook.bids,
            asks = orderBook.asks,
            selectedPrice = selectedPrice,
            onPriceSelected = { price -> onPriceSelected(price) },
            modifier = Modifier.weight(1f)
        )

        // Панель ордера
        OrderPlacementPanel(
            selectedPrice = selectedPrice,
            orderQuantity = orderQuantity,
            onQuantityChanged = onQuantityChanged,
            onPlaceOrder = onPlaceOrder,
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
        )
    }
}