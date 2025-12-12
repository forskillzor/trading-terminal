package com.aandios.tradingterminal.ui.dom

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aandios.tradingterminal.domain.entities.OrderBookData
import com.aandios.tradingterminal.domain.entities.OrderSide
import com.aandios.tradingterminal.ui.components.TerminalButton
import kotlin.math.abs

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

@Composable
private fun DomHeader(
    symbol: String,
    timestamp: Long,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "DOM - $symbol",
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.labelMedium
            )

            Text(
                text = "Live",
                color = Color.Green,
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}

@Composable
private fun DomContent(
    orderBook: OrderBookData,
    selectedPrice: Double?,
    mousePosition: Offset?,
    orderQuantity: String,
    onQuantityChanged: (String) -> Unit,
    onPlaceOrder: (OrderSide) -> Unit,
    modifier: Modifier = Modifier
) {
    val textMeasurer = rememberTextMeasurer()

    Column(
        modifier = modifier.fillMaxSize()
    ) {
        // ASKS (продажи - красные)
        DomSection(
            title = "ASKS",
            levels = orderBook.asks,
            isAsk = true,
            selectedPrice = selectedPrice,
            textMeasurer = textMeasurer,
            modifier = Modifier.weight(1f)
        )

        // Spread (разница)
        DomSpread(
            bestBid = orderBook.bids.firstOrNull()?.price,
            bestAsk = orderBook.asks.firstOrNull()?.price,
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
            textMeasurer = textMeasurer,
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

@Composable
private fun DomSection(
    title: String,
    levels: List<com.aandios.tradingterminal.domain.entities.OrderBookLevel>,
    isAsk: Boolean,
    selectedPrice: Double?,
    textMeasurer: TextMeasurer,
    modifier: Modifier = Modifier
) {
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
    val onSurface = MaterialTheme.colorScheme.onSurface
    val outlineVariant = MaterialTheme.colorScheme.outlineVariant
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant

    Column(modifier = modifier.fillMaxSize()) {
        // Заголовок секции
        Surface(
            color = if (isAsk) Color.Red.copy(alpha = 0.1f)
            else Color.Green.copy(alpha = 0.1f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = title,
                color = if (isAsk) MaterialTheme.colorScheme.secondary
                else MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }

        // Колонки заголовков
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Price",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelSmall
            )
            Text(
                text = "Size",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelSmall
            )
            Text(
                text = "Total",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelSmall
            )
        }

        // Список уровней
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp)
        ) {
            if (levels.isNotEmpty()) {
                val maxTotal = levels.maxOfOrNull { it.total } ?: 0.0
                val levelHeight = size.height / (levels.size + 1)

                levels.forEachIndexed { index, level ->
                    val y = index * levelHeight

                    // Фон для выделенной цены
                    if (selectedPrice != null &&
                        abs(level.price - selectedPrice) < 0.000001) {
                        drawRect(
                            color = Color.Yellow.copy(alpha = 0.2f),
                            topLeft = Offset(0f, y),
                            size = Size(size.width, levelHeight)
                        )
                    }

                    // Градиент объема
                    val volumeWidth = (level.total / maxTotal) * size.width * 0.3f
                    val volumeColor = if (isAsk)
                        Color.Red.copy(alpha = 0.15f)
                    else
                        Color.Green.copy(alpha = 0.15f)

                    drawRect(
                        color = volumeColor,
                        topLeft = Offset(0f, y),
                        size = Size(volumeWidth.toFloat(), levelHeight)
                    )

                    // Текст: цена
                    val priceText = formatDomPrice(level.price)
                    val priceColor = if (isAsk)
                        secondary
                    else
                        primary

                    drawText(
                        textMeasurer = textMeasurer,
                        text = priceText,
                        style = TextStyle(
                            color = priceColor,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        ),
                        topLeft = Offset(0f, y + 4f)
                    )

                    // Текст: размер
                    val sizeText = String.format("%.3f", level.quantity)
                    drawText(
                        textMeasurer = textMeasurer,
                        text = sizeText,
                        style = TextStyle(
                            onSurface,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        ),
                        topLeft = Offset(size.width * 0.4f, y + 4f)
                    )

                    // Текст: тотал
                    val totalText = String.format("%.1f", level.total)
                    drawText(
                        textMeasurer = textMeasurer,
                        text = totalText,
                        style = TextStyle(
                            onSurfaceVariant,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        ),
                        topLeft = Offset(size.width * 0.7f, y + 4f)
                    )

                    // Разделительная линия
                    drawLine(
                        outlineVariant,
                        start = Offset(0f, y + levelHeight),
                        end = Offset(size.width, y + levelHeight),
                        strokeWidth = 0.5f
                    )
                }
            }
        }
    }
}

@Composable
private fun DomSpread(
    bestBid: Double?,
    bestAsk: Double?,
    modifier: Modifier = Modifier
) {
    val spread = if (bestBid != null && bestAsk != null) {
        bestAsk - bestBid
    } else null

    val spreadPercent = if (bestBid != null && bestAsk != null && bestBid > 0) {
        (spread!! / bestBid) * 100
    } else null

    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = modifier
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Best Bid
            Column(
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = "BID",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelSmall
                )
                bestBid?.let {
                    Text(
                        text = formatDomPrice(it),
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            // Spread
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "SPREAD",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelSmall
                )
                spread?.let {
                    Text(
                        text = String.format("%.2f", it),
                        color = Color.Yellow,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                spreadPercent?.let {
                    Text(
                        text = String.format("(%.4f%%)", it),
                        color = Color.Yellow,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }

            // Best Ask
            Column(
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = "ASK",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelSmall
                )
                bestAsk?.let {
                    Text(
                        text = formatDomPrice(it),
                        color = MaterialTheme.colorScheme.secondary,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

@Composable
private fun OrderPlacementPanel(
    selectedPrice: Double?,
    orderQuantity: String,
    onQuantityChanged: (String) -> Unit,
    onPlaceOrder: (OrderSide) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Выбранная цена
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Price:",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelMedium
                )
                Text(
                    text = selectedPrice?.let { formatDomPrice(it) } ?: "--",
                    color = if (selectedPrice != null) Color.Yellow
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            // Поле ввода количества
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Qty:",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelMedium
                )
                BasicTextField(
                    value = orderQuantity,
                    onValueChange = onQuantityChanged,
                    modifier = Modifier
                        .weight(1f)
                        .height(32.dp)
                        .background(
                            color = MaterialTheme.colorScheme.surface,
                            shape = MaterialTheme.shapes.small
                        )
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.outlineVariant,
                            shape = MaterialTheme.shapes.small
                        )
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    textStyle = TextStyle(
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 13.sp,
                        fontFamily = FontFamily.Monospace
                    ),
                    singleLine = true,
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary)
                )            }

            // Кнопки размещения ордеров
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                TerminalButton(
                    onClick = { onPlaceOrder(OrderSide.BUY) },
                    isActive = false,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "BUY",
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.labelMedium
                    )
                }

                TerminalButton(
                    onClick = { onPlaceOrder(OrderSide.SELL) },
                    isActive = false,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "SELL",
                        color = MaterialTheme.colorScheme.secondary,
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }
        }
    }
}

private fun formatDomPrice(price: Double): String {
    return when {
        price >= 1000 -> String.format("%.2f", price)
        price >= 100 -> String.format("%.3f", price)
        price >= 10 -> String.format("%.4f", price)
        price >= 1 -> String.format("%.5f", price)
        else -> String.format("%.6f", price)
    }
}

private fun findPriceAtPosition(
    offset: Offset,
    orderBook: OrderBookData,
    containerHeight: Float
): Double? {
    val asksHeight = containerHeight * 0.4f // 40% на asks
    val spreadHeight = 32f // Высота спреда
    val bidsHeight = containerHeight * 0.4f // 40% на bids
    val orderPanelHeight = 120f // Высота панели ордеров

    val availableHeight = containerHeight - orderPanelHeight
    val sectionHeight = availableHeight * 0.4f

    if (offset.y < sectionHeight) {
        // ASKS секция
        val levelHeight = sectionHeight / (orderBook.asks.size + 1)
        val levelIndex = (offset.y / levelHeight).toInt()

        if (levelIndex < orderBook.asks.size) {
            return orderBook.asks[levelIndex].price
        }
    } else if (offset.y > sectionHeight + spreadHeight &&
        offset.y < sectionHeight * 2 + spreadHeight) {
        // BIDS секция
        val bidsStartY = sectionHeight + spreadHeight
        val relativeY = offset.y - bidsStartY
        val levelHeight = sectionHeight / (orderBook.bids.size + 1)
        val levelIndex = (relativeY / levelHeight).toInt()

        if (levelIndex < orderBook.bids.size) {
            return orderBook.bids[levelIndex].price
        }
    }

    return null
}