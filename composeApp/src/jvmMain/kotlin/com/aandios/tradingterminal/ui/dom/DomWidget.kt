package com.aandios.tradingterminal.ui.dom

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.aandios.tradingterminal.domain.entities.OrderBookData
import com.aandios.tradingterminal.domain.entities.OrderSide

@Composable
fun DomWidget(
    orderBook: OrderBookData?,
    selectedPrice: Double?,
    onPriceSelected: (Double?) -> Unit,
    orderQuantity: String,
    onQuantityChanged: (String) -> Unit,
    onPlaceOrder: (OrderSide) -> Unit,
    modifier: Modifier = Modifier,
    width: Dp = 300.dp,
    showHeader: Boolean = true
) {
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
                selectedPrice = selectedPrice,
                onPriceSelected = onPriceSelected,
                orderQuantity = orderQuantity,
                onQuantityChanged = onQuantityChanged,
                onPlaceOrder = onPlaceOrder,
                modifier = Modifier.weight(1f)
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