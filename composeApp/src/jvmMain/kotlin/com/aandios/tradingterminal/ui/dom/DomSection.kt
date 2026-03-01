package com.aandios.tradingterminal.ui.dom

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aandios.tradingterminal.domain.entities.OrderBookLevel
import kotlin.math.abs

@Composable
fun DomSection(
    title: String,
    levels: List<OrderBookLevel>,
    isAsk: Boolean,
    selectedPrice: Double?,
    onPriceClick: (Double) -> Unit,
    modifier: Modifier = Modifier
) {
    val maxTotal = remember(levels) {
        levels.maxOfOrNull { it.total.toDouble() } ?: 0.0
    }

    // Стабилизируем цвета
    val surfaceColor = if (isAsk)
        MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f)
    else
        MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)

    val titleColor = if (isAsk)
        MaterialTheme.colorScheme.secondary
    else
        MaterialTheme.colorScheme.primary

    Column(modifier = modifier.fillMaxSize()) {
        // Заголовок секции
        Surface(
            color = surfaceColor,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = title,
                color = titleColor,
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

        // Список уровней с оптимизациями
        LazyColumn(
            modifier = Modifier.fillMaxSize()
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
                    maxTotal = maxTotal,
                    onPriceClick = { onPriceClick(price) }
                )
            }
        }
    }
}
@Composable
private fun DomLevelRow(
    level: OrderBookLevel,
    isAsk: Boolean,
    isSelected: Boolean,
    maxTotal: Double,
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
    val volumeWidth = remember(level.total, maxTotal) {
        val total = level.quantity.toDoubleOrNull() ?: 0.0
        (total / maxTotal).coerceIn(0.0, 1.0).toFloat()
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
        Text(
            text = formattedQuantity,
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.bodySmall.copy(
                fontFamily = FontFamily.Monospace
            ),
            modifier = Modifier.weight(1f)
        )

        // Total с визуализацией
        Box(
            modifier = Modifier
                .weight(1f)
                .height(16.dp)
        ) {
            // Градиент объема
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawRect(
                    color = if (isAsk)
                        Color.Red.copy(alpha = 0.2f)
                    else
                        Color.Green.copy(alpha = 0.2f),
                    size = Size(size.width * volumeWidth, size.height)
                )
            }

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