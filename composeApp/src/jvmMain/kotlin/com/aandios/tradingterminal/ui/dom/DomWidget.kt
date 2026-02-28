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
    var mousePosition by remember { mutableStateOf<Offset?>(null) }

    Column(
        modifier = modifier
            .width(width)
            .fillMaxHeight()
            .background(MaterialTheme.colorScheme.surface)
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { offset ->
                        mousePosition = offset
                        // Определяем, на какую цену кликнули
                        orderBook?.let { data ->
                            val price = findPriceAtPosition(
                                offset = offset,
                                orderBook = data,
                                containerHeight = size.height.toFloat()
                            )
                            onPriceSelected(price)
                        }
                    }
                )
            }
    ) {
        if (showHeader) {
            DomHeader(
                symbol = orderBook?.symbol ?: "",
                timestamp = orderBook?.timestamp ?: 0
            )
        }

        if (orderBook != null) {
            DomContent(
                orderBook = orderBook,
                selectedPrice = selectedPrice,
                mousePosition = mousePosition,
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


private fun findPriceAtPosition(
    offset: Offset,
    orderBook: OrderBookData,
    containerHeight: Float
): Double? {
    val orderPanelHeight = 120f  // Высота панели ордеров
    val spreadHeight = 32f        // Высота спреда

    // Доступная высота для секций ASKS и BIDS (без панели ордеров)
    val availableHeight = containerHeight - orderPanelHeight

    // Высота каждой секции (ASKS и BIDS) - они равны
    val sectionHeight = (availableHeight - spreadHeight) / 2

    // Определяем границы секций
    val asksStartY = 0f
    val asksEndY = sectionHeight

    val spreadStartY = asksEndY
    val spreadEndY = spreadStartY + spreadHeight

    val bidsStartY = spreadEndY
    val bidsEndY = bidsStartY + sectionHeight

    when (// Клик в секции ASKS
        offset.y) {
        in asksStartY..asksEndY -> {
            val levelHeight = sectionHeight / orderBook.asks.size
            val levelIndex = (offset.y / levelHeight).toInt()

            return if (levelIndex in orderBook.asks.indices) {
                orderBook.asks[levelIndex].price
            } else null
        }

        // Клик в секции BIDS
        in bidsStartY..bidsEndY -> {
            val relativeY = offset.y - bidsStartY
            val levelHeight = sectionHeight / orderBook.bids.size
            val levelIndex = (relativeY / levelHeight).toInt()

            return if (levelIndex in orderBook.bids.indices) {
                orderBook.bids[levelIndex].price
            } else null
        }

        // Клик в области спреда - игнорируем или выбираем ближайшую цену
        in spreadStartY..spreadEndY -> {
            // Можно вернуть null или выбрать ближайшую цену
            return null
        }
    }

    return null
}