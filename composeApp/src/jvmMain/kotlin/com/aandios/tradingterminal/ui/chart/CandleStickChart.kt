package com.aandios.tradingterminal.ui.chart

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aandios.tradingterminal.domain.entities.Candle

// Data classes на уровне файла
data class PriceRange(
    val max: Float,
    val min: Float,
    val visibleMax: Float,
    val visibleMin: Float,
    val range: Float
)

data class CandleMetrics(
    val width: Float,
    val spacing: Float
)

@Composable
fun CandleStickChart(
    candles: List<Candle>,
    modifier: Modifier = Modifier,
    config: ChartConfig = DefaultChartConfig,
    showPriceScale: Boolean = true,
    priceScaleWidth: Dp = 60.dp
) {
    if (candles.isEmpty()) return

    // Расчет минимальной и максимальной цены
    val priceRange = remember(candles) {
        if (candles.isEmpty()) {
            PriceRange(0f, 0f, 0f, 0f, 0f)
        } else {
            calculatePriceRange(candles)
        }
    }

    // Если нужна шкала цен
    if (showPriceScale) {
        Row(
            modifier = modifier
                .fillMaxSize()
                .background(config.backgroundColor)
        ) {
            // Сам график свечей справа
            Canvas(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize()
                    .background(config.backgroundColor)
                    .clipToBounds()
            ) {
                drawChart(candles, priceRange, config)
            }
            // Вертикальная разделительная линия
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .fillMaxHeight()
                    .background(config.gridColor.copy(alpha = 0.5f))
            )
            // Шкала цен справа
            SimplePriceScale(
                priceRange = priceRange,
                modifier = Modifier
                    .width(priceScaleWidth)
                    .fillMaxHeight(),
                config = config
            )
        }
    } else {
        Canvas(
            modifier = modifier
                .fillMaxSize()
                .background(config.backgroundColor)
                .clipToBounds()
        ) {
            drawChart(candles, priceRange, config)
        }
    }
}

// Функция расчета диапазона цен
private fun calculatePriceRange(candles: List<Candle>): PriceRange {
    if (candles.isEmpty()) {
        return PriceRange(0f, 0f, 0f, 0f, 0f)
    }

    val maxPrice = candles.maxOf { it.high }
    val minPrice = candles.minOf { it.low }
    val priceRange = maxPrice - minPrice

    // Добавляем 10% padding сверху и снизу
    val padding = priceRange * 0.1f
    val visibleMax = maxPrice + padding
    val visibleMin = minPrice - padding
    val visibleRange = visibleMax - visibleMin

    return PriceRange(
        max = maxPrice,
        min = minPrice,
        visibleMax = visibleMax,
        visibleMin = visibleMin,
        range = visibleRange
    )
}

// Функция для отрисовки всего графика (внутри DrawScope)
private fun DrawScope.drawChart(
    candles: List<Candle>,
    priceRange: PriceRange,
    config: ChartConfig
) {
    // Рисуем сетку если нужно
    drawGrid(config)

    // Рассчитываем ширину свечи
    val candleMetrics = calculateCandleMetrics(candles.size)

    // Рисуем каждую свечу
    candles.forEachIndexed { index, candle ->
        val x = index * (candleMetrics.width + candleMetrics.spacing) + candleMetrics.width / 2

        drawCandle(
            candle = candle,
            centerX = x,
            priceRange = priceRange,
            metrics = candleMetrics,
            config = config
        )
    }
}

// Функция для расчета метрик свечей (внутри DrawScope)
private fun DrawScope.calculateCandleMetrics(candleCount: Int): CandleMetrics {
    val availableWidth = size.width * 0.9f // 90% ширины для свечей, 10% для отступов
    val totalWidth = availableWidth / candleCount
    val width = totalWidth * 0.7f // 70% ширины под свечу
    val spacing = totalWidth * 0.3f // 30% под промежутки

    return CandleMetrics(width, spacing)
}

// Функция для конвертации цены в Y координату (внутри DrawScope)
private fun DrawScope.priceToY(price: Float, priceRange: PriceRange): Float {
    return size.height - ((price - priceRange.visibleMin) / priceRange.range) * size.height
}

// Функция для рисования сетки (внутри DrawScope)
private fun DrawScope.drawGrid(config: ChartConfig) {
    if (!config.showGrid) return

    // Горизонтальные линии (уровни цен)
    val horizontalLines = 5
    for (i in 0..horizontalLines) {
        val y = size.height * i / horizontalLines.toFloat()

        drawLine(
            color = config.gridColor,
            start = Offset(0f, y),
            end = Offset(size.width, y),
            strokeWidth = 1f
        )
    }

    // Вертикальные линии (время)
    val verticalLines = 10
    for (i in 0..verticalLines) {
        val x = size.width * i / verticalLines.toFloat()

        drawLine(
            color = config.gridColor,
            start = Offset(x, 0f),
            end = Offset(x, size.height),
            strokeWidth = 1f
        )
    }
}

// Функция отрисовки свечи
private fun DrawScope.drawCandle(
    candle: Candle,
    centerX: Float,
    priceRange: PriceRange,
    metrics: CandleMetrics,
    config: ChartConfig
) {
    val style = config.candleStyle

    // Функция для конвертации цены в Y координату
    fun priceToY(price: Float): Float {
        return this.priceToY(price, priceRange)
    }

    val isBullish = candle.close >= candle.open
    val bodyColor = if (isBullish) style.bullishColor else style.bearishColor
    val shadowColor = style.shadowColor

    // Координаты для отрисовки
    val openY = priceToY(candle.open)
    val closeY = priceToY(candle.close)
    val highY = priceToY(candle.high)
    val lowY = priceToY(candle.low)

    // 1. Рисуем верхнюю тень (от high до максимума тела)
    if (style.showShadows) {
        val topOfBody = if (isBullish) closeY else openY
        if (highY < topOfBody) {
            drawLine(
                color = shadowColor,
                start = Offset(centerX, highY),
                end = Offset(centerX, topOfBody),
                strokeWidth = style.shadowWidth
            )
        }

        // 2. Рисуем нижнюю тень (от low до минимума тела)
        val bottomOfBody = if (isBullish) openY else closeY
        if (lowY > bottomOfBody) {
            drawLine(
                color = shadowColor,
                start = Offset(centerX, bottomOfBody),
                end = Offset(centerX, lowY),
                strokeWidth = style.shadowWidth
            )
        }
    }

    // 3. Рисуем тело свечи
    val bodyTop = if (isBullish) closeY else openY
    val bodyBottom = if (isBullish) openY else closeY
    val bodyHeight = kotlin.math.abs(bodyBottom - bodyTop)

    if (bodyHeight > 0) {
        drawRect(
            color = bodyColor,
            topLeft = Offset(centerX - metrics.width / 2, bodyTop),
            size = androidx.compose.ui.geometry.Size(metrics.width, bodyHeight)
        )
    } else {
        // Для Doji свечей (open == close) рисуем линию
        drawLine(
            color = bodyColor,
            start = Offset(centerX - metrics.width / 2, bodyTop),
            end = Offset(centerX + metrics.width / 2, bodyTop),
            strokeWidth = 2f
        )
    }
}

// Простая шкала цен
@Composable
private fun SimplePriceScale(
    priceRange: PriceRange,
    modifier: Modifier = Modifier,
    config: ChartConfig,
    numberOfLevels: Int = 8
) {
    Box(
        modifier = modifier
            .background(config.backgroundColor)
            .padding(horizontal = 4.dp),
        contentAlignment = Alignment.CenterEnd
    ) {
        Column(
            modifier = Modifier.fillMaxHeight(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Генерируем цены для шкалы (от максимума к минимуму)
            val priceLevels = remember(priceRange, numberOfLevels) {
                generatePriceLevels(
                    min = priceRange.visibleMin,
                    max = priceRange.visibleMax,
                    count = numberOfLevels
                )
            }

            // Отображаем цены
            priceLevels.forEach { price ->
                Text(
                    text = formatPrice(price),
                    color = config.axisTextColor,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Normal,
                    modifier = Modifier.padding(vertical = 2.dp)
                )
            }
        }
    }
}

// Функция для генерации уровней цен
private fun generatePriceLevels(min: Float, max: Float, count: Int): List<Float> {
    if (count <= 0) return emptyList()
    if (count == 1) return listOf(max)

    val range = max - min
    val step = range / (count - 1)

    return List(count) { i ->
        max - (step * i) // От максимума к минимуму
    }
}

// Функция форматирования цены
private fun formatPrice(price: Float): String {
    return when {
        price >= 1000 -> String.format("%.1f", price)
        price >= 100 -> String.format("%.2f", price)
        price >= 10 -> String.format("%.3f", price)
        price >= 1 -> String.format("%.4f", price)
        else -> String.format("%.6f", price)
    }
}