package com.aandios.tradingterminal.ui.chart

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.aandios.tradingterminal.domain.entities.Candle

@Composable
fun CandleStickChart(
    candles: List<Candle>,
    modifier: Modifier = Modifier,
    config: ChartConfig = DefaultChartConfig
) {
    if (candles.isEmpty()) return

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .background(config.backgroundColor)
    ) {
        // Рисуем сетку если нужно
        if (config.showGrid) {
            drawGrid(config)
        }

        // Находим min/max цены для масштабирования
        val priceRange = calculatePriceRange(candles)

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
}

// Вспомогательные data class
private data class PriceRange(
    val max: Float,
    val min: Float,
    val visibleMax: Float,
    val visibleMin: Float,
    val range: Float
)

private data class CandleMetrics(
    val width: Float,
    val spacing: Float
)

// Вспомогательные функции
private fun DrawScope.calculatePriceRange(candles: List<Candle>): PriceRange {
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

private fun DrawScope.calculateCandleMetrics(candleCount: Int): CandleMetrics {
    val availableWidth = size.width * 0.9f // 90% ширины для свечей, 10% для отступов
    val totalWidth = availableWidth / candleCount
    val width = totalWidth * 0.7f // 70% ширины под свечу
    val spacing = totalWidth * 0.3f // 30% под промежутки

    return CandleMetrics(width, spacing)
}

private fun DrawScope.drawGrid(config: ChartConfig) {
    // Горизонтальные линии (уровни цен)
    val gridLines = 5
    for (i in 0..gridLines) {
        val y = size.height * i / gridLines.toFloat()

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
        return size.height - ((price - priceRange.visibleMin) / priceRange.range) * size.height
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
        // Для Doji свечи (open == close) рисуем линию
        drawLine(
            color = bodyColor,
            start = Offset(centerX - metrics.width / 2, bodyTop),
            end = Offset(centerX + metrics.width / 2, bodyTop),
            strokeWidth = 2f
        )
    }
}