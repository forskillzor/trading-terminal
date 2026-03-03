package com.aandios.tradingterminal.ui.dom

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aandios.tradingterminal.data.api.binance.models.BestPrices
import com.aandios.tradingterminal.domain.entities.OrderBookLevel
import kotlin.math.abs
import kotlin.math.max

@Composable
fun NinjaTraderDom(
    bids: List<OrderBookLevel>,
    asks: List<OrderBookLevel>,
    bestPrices: BestPrices?,
    selectedPrice: Double?,
    onPriceSelected: (Double) -> Unit,
    modifier: Modifier = Modifier
) {
    // Находим максимальный объем для масштабирования
    val maxVolume = remember(bids, asks) {
        max(
            bids.maxOfOrNull { it.quantity.toDouble() } ?: 0.0,
            asks.maxOfOrNull { it.quantity.toDouble() } ?: 0.0
        )
    }

    Column(modifier = modifier.fillMaxSize()) {
        // Заголовок
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Bid Vol",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.weight(0.8f)
            )
            Text(
                text = "Price",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.weight(0.6f)
            )
            Text(
                text = "Ask Vol",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.weight(0.8f)
            )
        }

        // ASKS (сверху)
        LazyColumn(
            modifier = Modifier.weight(1f),
            reverseLayout = true // Asks от лучшей (мин) к худшей (макс)
        ) {
            items(
                items = asks,
                key = { "ask-${it.price}" }
            ) { level ->
                NinjaTraderRow(
                    level = level,
                    isAsk = true,
                    maxVolume = maxVolume,
                    selectedPrice = selectedPrice,
                    onPriceClick = onPriceSelected
                )
            }
        }

        // Spread (разница)
        val bestBid = bids.firstOrNull()?.price?.toDoubleOrNull()
        val bestAsk = asks.firstOrNull()?.price?.toDoubleOrNull()

        if (bestBid != null && bestAsk != null) {
            DomSpread(
                bestPrices = bestPrices,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)  // Чуть выше для отображения объемов
            )
        }

        // BIDS (снизу)
        LazyColumn(
            modifier = Modifier.weight(1f)
        ) {
            items(
                items = bids,
                key = { "bid-${it.price}" }
            ) { level ->
                NinjaTraderRow(
                    level = level,
                    isAsk = false,
                    maxVolume = maxVolume,
                    selectedPrice = selectedPrice,
                    onPriceClick = onPriceSelected
                )
            }
        }
    }
}

@Composable
private fun NinjaTraderRow(
    level: OrderBookLevel,
    isAsk: Boolean,
    maxVolume: Double,
    selectedPrice: Double?,
    onPriceClick: (Double) -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()

    val price = level.price.toDoubleOrNull() ?: return
    val quantity = level.quantity.toDoubleOrNull() ?: 0.0
    val isSelected = selectedPrice?.let { abs(it - price) < 0.000001 } ?: false

    // Цвета
    val backgroundColor = when {
        isSelected -> Color.Yellow.copy(alpha = 0.3f)
        isHovered -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        else -> Color.Transparent
    }

    val priceColor = if (isAsk)
        MaterialTheme.colorScheme.secondary
    else
        MaterialTheme.colorScheme.primary

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .hoverable(interactionSource)
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) { onPriceClick(price) }
            .background(backgroundColor)
            .padding(horizontal = 8.dp, vertical = 1.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Bid Volume (слева - только для Bids)
        Box(
            modifier = Modifier
                .weight(0.8f)
                .height(20.dp)
        ) {
            if (!isAsk && quantity > 0) {
                // Горизонтальный объем для Bid
                val volumeWidth = (quantity / maxVolume).coerceIn(0.0, 1.0)
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(volumeWidth.toFloat())
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                )
            }
            Text(
                text = if (!isAsk) formatVolume(quantity) else "",
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp
                ),
                modifier = Modifier.align(Alignment.CenterStart)
            )
        }

        // Price (центр)
        Text(
            text = formatPrice(price),
            color = priceColor,
            style = MaterialTheme.typography.bodySmall.copy(
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
            ),
            modifier = Modifier.weight(0.6f)
        )

        // Ask Volume (справа - только для Asks)
        Box(
            modifier = Modifier
                .weight(0.8f)
                .height(20.dp)
        ) {
            if (isAsk && quantity > 0) {
                // Горизонтальный объем для Ask
                val volumeWidth = (quantity / maxVolume).coerceIn(0.0, 1.0)
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(volumeWidth.toFloat())
                        .align(Alignment.CenterEnd)
                        .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f))
                )
            }
            Text(
                text = if (isAsk) formatVolume(quantity) else "",
                color = MaterialTheme.colorScheme.secondary,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp
                ),
                modifier = Modifier.align(Alignment.CenterEnd)
            )
        }
    }
}

@Composable
private fun NinjaTraderSpread(
    bestBid: Double,
    bestAsk: Double,
    modifier: Modifier = Modifier
) {
    val spread = bestAsk - bestBid
    val spreadPercent = (spread / bestBid) * 100

    Row(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = formatPrice(bestBid),
            color = MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.bodySmall.copy(
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "SPREAD",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelSmall
            )
            Text(
                text = "${formatPrice(spread)} (${"%.2f".format(spreadPercent)}%)",
                color = Color.Yellow,
                style = MaterialTheme.typography.labelSmall
            )
        }

        Text(
            text = formatPrice(bestAsk),
            color = MaterialTheme.colorScheme.secondary,
            style = MaterialTheme.typography.bodySmall.copy(
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
        )
    }
}

private fun formatPrice(price: Double): String {
    return when {
        price >= 1000 -> String.format("%.2f", price)
        price >= 100 -> String.format("%.3f", price)
        price >= 10 -> String.format("%.4f", price)
        price >= 1 -> String.format("%.5f", price)
        else -> String.format("%.6f", price)
    }
}

private fun formatVolume(volume: Double): String {
    return when {
        volume >= 1000 -> String.format("%.1fk", volume / 1000)
        volume >= 100 -> String.format("%.0f", volume)
        volume >= 10 -> String.format("%.1f", volume)
        else -> String.format("%.2f", volume)
    }
}