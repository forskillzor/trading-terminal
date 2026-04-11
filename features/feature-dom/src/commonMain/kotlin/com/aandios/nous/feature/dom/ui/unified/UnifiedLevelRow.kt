package com.aandios.nous.feature.dom.ui.unified

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.aandios.nous.api.market.model.orderbook.OrderBookLevel
import kotlin.math.abs

@Composable
fun UnifiedLevelRow(
    level: OrderBookLevel,
    maxVolume: Double,
    selectedPrice: Double?,
    bestBid: Double?,
    bestAsk: Double?,
    onPriceClick: (Double) -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()

    val price = level.price.toDoubleOrNull() ?: return
    val bidQty = level.bidQty.toDoubleOrNull() ?: 0.0
    val askQty = level.askQty.toDoubleOrNull() ?: 0.0
    val isSelected = selectedPrice?.let { abs(it - price) < 0.000001 } ?: false
    val isBestBid = bestBid?.let { abs(it - price) < 0.000001 } ?: false
    val isBestAsk = bestAsk?.let { abs(it - price) < 0.000001 } ?: false
    val isBestPrice = isBestBid || isBestAsk

    // Цвета
    val backgroundColor = when {
        isSelected -> Color.Yellow.copy(alpha = 0.3f)
        isHovered -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        else -> Color.Transparent
    }
    // Подсветка лучших цен: тонкая граница
    val borderColor = when {
        isBestBid -> MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
        isBestAsk -> MaterialTheme.colorScheme.secondary.copy(alpha = 0.7f)
        else -> Color.Transparent
    }
    val borderWidth = if (isBestPrice) 1.dp else 0.dp

    val priceColor = Color.White // Белый цвет для текста цены

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .hoverable(interactionSource)
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) { onPriceClick(price) }
            .background(backgroundColor)
            .border(borderWidth, borderColor, shape = RoundedCornerShape(2.dp))
            .padding(horizontal = 8.dp, vertical = 1.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Bid Volume (слева)
        Box(
            modifier = Modifier
                .weight(0.8f)
                .height(20.dp)
        ) {
            if (bidQty > 0) {
                // Горизонтальный объем для Bid
                val volumeWidth = (bidQty / maxVolume).coerceIn(0.0, 1.0)
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(volumeWidth.toFloat())
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                )
            }
            if (bidQty > 0) {
                Text(
                    text = formatVolume(bidQty),
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp
                    ),
                    modifier = Modifier.align(Alignment.CenterStart)
                )
            }
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

        // Ask Volume (справа)
        Box(
            modifier = Modifier
                .weight(0.8f)
                .height(20.dp)
        ) {
            if (askQty > 0) {
                // Горизонтальный объем для Ask
                val volumeWidth = (askQty / maxVolume).coerceIn(0.0, 1.0)
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(volumeWidth.toFloat())
                        .align(Alignment.CenterEnd)
                        .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f))
                )
            }
            if (askQty > 0) {
                Text(
                    text = formatVolume(askQty),
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
}