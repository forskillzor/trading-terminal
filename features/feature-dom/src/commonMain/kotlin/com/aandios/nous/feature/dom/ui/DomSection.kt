package com.aandios.nous.feature.dom.ui

import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.aandios.nous_platform.domain.entities.OrderBookLevel
import kotlin.math.abs

@Composable
fun DomSection(
    levels: List<OrderBookLevel>,
    maxVolume: Double,
    isAsk: Boolean,
    selectedPrice: Double?,
    onPriceClick: (Double) -> Unit,
    modifier: Modifier = Modifier
) {

    // Список уровней с оптимизациями
    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
    ) {
        items(
            items = levels,
            key = { it.price } // Ключ по цене - стабильный!
        ) { level ->
            val price = level.price.toDoubleOrNull() ?: return@items
            val isSelected = selectedPrice?.let { abs(it - price) < 0.000001 } ?: false

            DomLevelRow(
                level = level,
                isAsk = isAsk,
                isSelected = isSelected,
                maxVolume = maxVolume,
                totalMax = maxVolume,
                onPriceClick = { onPriceClick(price) }
            )
        }
    }
}

@Composable
private fun DomLevelRow(
    level: OrderBookLevel,
    isAsk: Boolean,
    isSelected: Boolean,
    maxVolume: Double,
    totalMax:Double,
    onPriceClick: () -> Unit
) {
    // Создаем InteractionSource для отслеживания hover
    val interactionSource = remember { MutableInteractionSource() }

    // Состояние hover
    val isHovered by interactionSource.collectIsHoveredAsState()

    // Стабилизируем цвета
    val priceColor = if (isAsk)
        MaterialTheme.colorScheme.secondary
    else
        MaterialTheme.colorScheme.primary

    val backgroundColor = when {
        isSelected -> Color.Yellow.copy(alpha = 0.3f)
        isHovered -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        else -> Color.Transparent
    }

    // Форматируем значения один раз
    val formattedPrice = remember(level.price) {
        formatDomPrice(level.price.toDoubleOrNull() ?: 0.0)
    }
    val formattedQuantity = remember(level.quantity) {
        String.format("%.3f", level.quantity.toDoubleOrNull() ?: 0.0)
    }
    val formattedTotal = remember(level.total) {
        String.format("%.1f", level.total.toDoubleOrNull() ?: 0.0)
    }
    val volumeWidth = remember(level.total, maxVolume) {
        val total = level.quantity.toDoubleOrNull() ?: 0.0
        (total / maxVolume).coerceIn(0.0, 1.0).toFloat()
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            // Добавляем hoverable с interactionSource
            .hoverable(interactionSource = interactionSource)
            // Клик с interactionSource
            .clickable(
                interactionSource = interactionSource,
                indication = null // Убираем ripple для производительности
            ) {
                println("📊 Level clicked: ${level.price}")
                onPriceClick()
            }
            .background(backgroundColor)
            .padding(horizontal = 8.dp, vertical = 1.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Price
        Text(
            text = formattedPrice,
            color = priceColor,
            style = MaterialTheme.typography.bodySmall.copy(
                fontFamily = FontFamily.Monospace
            ),
            modifier = Modifier.weight(1f)
        )

        // Quantity
        Box(
            modifier = Modifier
                .weight(1f)
                .height(16.dp)
        ) {
            // Градиент объема
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawRect(
                    color = if (isAsk)
                        Color.Red.copy(alpha = 0.3f)
                    else
                        Color.Green.copy(alpha = 0.3f),
                    size = Size(size.width * volumeWidth, size.height)
                )
            }
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawRect(
                    color = if (isAsk)
                        Color.Red.copy(alpha = 0.1f)
                    else
                        Color.Green.copy(alpha = 0.1f),
                    size = Size(size.width * volumeWidth, size.height)
                )
            }

            Text(
                text = formattedQuantity,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontFamily = FontFamily.Monospace
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 4.dp)
                    .align(Alignment.CenterStart)
            )
        }

        // Total с визуализацией
        Box(
            modifier = Modifier
                .weight(1f)
                .height(16.dp)
        ) {
            // Градиент объема
//            Canvas(modifier = Modifier.fillMaxSize()) {
//                drawRect(
//                    color = if (isAsk)
//                        Color.Red.copy(alpha = 0.2f)
//                    else
//                        Color.Green.copy(alpha = 0.2f),
//                    size = Size(size.width * volumeWidth, size.height)
//                )
//            }

            Text(
                text = formattedTotal,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = FontFamily.Monospace
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 4.dp)
                    .align(Alignment.CenterStart)
            )
        }
    }
}