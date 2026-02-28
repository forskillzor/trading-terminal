package com.aandios.tradingterminal.ui.dom

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import com.aandios.tradingterminal.domain.entities.OrderBookData
import com.aandios.tradingterminal.domain.entities.OrderSide

@Composable
fun DomContent(
    orderBook: OrderBookData,
    selectedPrice: Double?,
    onPriceSelected: (Double?) -> Unit,
    orderQuantity: String,
    onQuantityChanged: (String) -> Unit,
    onPlaceOrder: (OrderSide) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxSize()
    ) {
        // ASKS (продажи - красные)
        DomSection(
            title = "ASKS",
            levels = orderBook.asks.sortedByDescending { it.price.toDouble() },
            isAsk = true,
            selectedPrice = selectedPrice,
            onPriceClick = { price -> onPriceSelected(price) },
            modifier = Modifier.weight(1f)
        )

        // Spread (разница)
        DomSpread(
            bestBid = orderBook.bids.firstOrNull()?.price?.toDouble(),
            bestAsk = orderBook.asks.firstOrNull()?.price?.toDouble(),
            modifier = Modifier
                .fillMaxWidth()
                .height(32.dp)
        )

        // BIDS (покупки - зеленые)
        DomSection(
            title = "BIDS",
            levels = orderBook.bids,
            isAsk = false,
            selectedPrice = selectedPrice,
            onPriceClick = { price -> onPriceSelected(price) },
            modifier = Modifier.weight(1f)
        )

        // Панель размещения ордера
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