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
    mousePosition: Offset?,
    orderQuantity: String,
    onQuantityChanged: (String) -> Unit,
    onPlaceOrder: (OrderSide) -> Unit,
    modifier: Modifier = Modifier.Companion
) {
    val textMeasurer = rememberTextMeasurer()

    Column(
        modifier = modifier.fillMaxSize()
    ) {
        // ASKS (продажи - красные)
        DomSection(
            title = "ASKS",
            // todo fix performance price list sorting
            levels = orderBook.asks.sortedByDescending{(price, quantity, total) -> price},
            isAsk = true,
            selectedPrice = selectedPrice,
            textMeasurer = textMeasurer,
            modifier = Modifier.Companion.weight(1f)
        )

        // Spread (разница)
        DomSpread(
            bestBid = orderBook.bids.firstOrNull()?.price,
            bestAsk = orderBook.asks.firstOrNull()?.price,
            modifier = Modifier.Companion
                .fillMaxWidth()
                .height(32.dp)
        )

        // BIDS (покупки - зеленые)
        DomSection(
            title = "BIDS",
            levels = orderBook.bids,
            isAsk = false,
            selectedPrice = selectedPrice,
            textMeasurer = textMeasurer,
            modifier = Modifier.Companion.weight(1f)
        )

        // Панель размещения ордера
        OrderPlacementPanel(
            selectedPrice = selectedPrice,
            orderQuantity = orderQuantity,
            onQuantityChanged = onQuantityChanged,
            onPlaceOrder = onPlaceOrder,
            modifier = Modifier.Companion
                .fillMaxWidth()
                .height(120.dp)
        )
    }
}