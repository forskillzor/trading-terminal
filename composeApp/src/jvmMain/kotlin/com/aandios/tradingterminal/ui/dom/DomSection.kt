package com.aandios.tradingterminal.ui.dom

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
    textMeasurer: TextMeasurer,
    modifier: Modifier = Modifier.Companion
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
                modifier = Modifier.Companion.padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }

        // Колонки заголовков
        Row(
            modifier = Modifier.Companion
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
            modifier = Modifier.Companion
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
                        abs(level.price - selectedPrice) < 0.000001
                    ) {
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