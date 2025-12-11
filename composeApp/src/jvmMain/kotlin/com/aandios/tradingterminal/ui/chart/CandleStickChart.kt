package com.aandios.tradingterminal.ui.chart

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aandios.tradingterminal.domain.entities.Candle
import kotlin.math.abs


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

data class ChartLayout(
    val canvasWidth: Float,
    val canvasHeight: Float,
    val priceScaleWidth: Float,
    val chartArea: Rect,
    val priceScaleArea: Rect,
    val chartPadding: Float = 8f,
    val timeScaleHeight: Float = 20f,
    val chartMainArea: Rect,
    val timeScaleArea: Rect
)

@Composable
fun CandleStickChart(
    candles: List<Candle>,
    currentPrice: Float? = null,
    modifier: Modifier = Modifier,
    config: ChartConfig = DefaultChartConfig,
    showPriceScale: Boolean = true,
    priceScaleWidth: Dp = 60.dp
) {
    if (candles.isEmpty()) return

    // Расчет минимальной и максимальной цены
    val priceRange = remember(candles, currentPrice) {
        calculatePriceRangeWithCurrentPrice(candles, currentPrice)
    }

    // TextMeasurer для измерения текста
    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(config.backgroundColor)
    ) {
        // Рассчитываем layout графика
        val layout = remember(priceScaleWidth) {
            val canvasWidth = constraints.maxWidth.toFloat()
            val canvasHeight = constraints.maxHeight.toFloat()
            val chartPadding = 8f // Отступ графика от шкалы
            val timeScaleHeight = 20f

            val priceScaleWidthPx = with(density) {
                priceScaleWidth.toPx()
            }

            // Область для шкалы цен
            val priceScaleArea = Rect(
                left = canvasWidth - priceScaleWidthPx,
                top = 0f,
                right = canvasWidth,
                bottom = canvasHeight
            )

            val timeScaleArea = Rect(
                left = 0f,
                top = canvasHeight - timeScaleHeight,
                right = canvasWidth - priceScaleWidthPx - chartPadding,
                bottom = canvasHeight
            )
            val chartMainArea = Rect(
                left = 0f,
                top = 0f,
                right = canvasWidth - priceScaleWidthPx - chartPadding,
                bottom = canvasHeight - timeScaleHeight
            )

            // Область для графика с отступом от шкалы
            val chartArea = Rect(
                left = 0f,
                top = 0f,
                right = canvasWidth - priceScaleWidthPx - chartPadding,
                bottom = canvasHeight
            )

            ChartLayout(
                canvasWidth = canvasWidth,
                canvasHeight = canvasHeight,
                priceScaleWidth = priceScaleWidthPx,
                chartArea = chartArea,
                priceScaleArea = priceScaleArea,
                chartPadding = chartPadding,
                timeScaleHeight = timeScaleHeight,
                chartMainArea = chartMainArea,
                timeScaleArea = timeScaleArea,
            )
        }

        // Основной Canvas для графика
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .clipToBounds()
        ) {
            // Рисуем график в основной области
            drawChart(
                candles = candles,
                priceRange = priceRange,
                config = config,
                chartArea = layout.chartArea,
                currentPrice = currentPrice,
                textMeasurer = textMeasurer
            )

            drawTimeScale(
                candles = candles,
                config = config,
                timeScaleArea = layout.timeScaleArea,
                textMeasurer = textMeasurer
            )

            // Рисуем шкалу цен
            if (showPriceScale) {
                drawPriceScale(
                    priceRange = priceRange,
                    config = config,
                    priceScaleArea = layout.priceScaleArea,
                    currentPrice = currentPrice,
                    textMeasurer = textMeasurer
                )

                // Разделительная линия между графиком и шкалой
                drawLine(
                    color = config.gridColor.copy(alpha = 0.5f),
                    start = Offset(layout.chartArea.right + layout.chartPadding, 0f),
                    end = Offset(layout.chartArea.right + layout.chartPadding, layout.canvasHeight),
                    strokeWidth = 1f
                )
            }
        }
    }
}

// Функция для отрисовки всего графика
private fun DrawScope.drawChart(
    candles: List<Candle>,
    priceRange: PriceRange,
    config: ChartConfig,
    chartArea: Rect,
    currentPrice: Float?,
    textMeasurer: TextMeasurer
) {
    // Сохраняем область рисования для графика
    withTransform({
        translate(left = chartArea.left, top = chartArea.top)
        clipRect(0f, 0f, chartArea.width, chartArea.height)
    }) {
        // Сначала сетка
        drawGrid(config, chartArea.width, chartArea.height)

        // Потом свечи
        val candleMetrics = calculateCandleMetrics(candles.size, chartArea.width)
        candles.forEachIndexed { index, candle ->
            val x = index * (candleMetrics.width + candleMetrics.spacing) + candleMetrics.width / 2
            drawCandle(
                candle = candle,
                centerX = x,
                priceRange = priceRange,
                metrics = candleMetrics,
                config = config,
                chartHeight = chartArea.height
            )
        }

        // Линия текущей цены
        if (currentPrice != null) {
            drawCurrentPriceLine(
                currentPrice = currentPrice,
                priceRange = priceRange,
                config = config,
                chartHeight = chartArea.height,
                chartWidth = chartArea.width
            )
        }
    }
}

// Функция для отрисовки шкалы цен
private fun DrawScope.drawPriceScale(
    priceRange: PriceRange,
    config: ChartConfig,
    priceScaleArea: Rect,
    currentPrice: Float?,
    textMeasurer: TextMeasurer
) {
    withTransform({
        translate(left = priceScaleArea.left, top = priceScaleArea.top)
        clipRect(0f, 0f, priceScaleArea.width, priceScaleArea.height)
    }) {
        val numberOfLevels = 8

        // Генерируем уровни цен
        val priceLevels = generatePriceLevels(
            min = priceRange.visibleMin,
            max = priceRange.visibleMax,
            count = numberOfLevels
        )

        // Сначала рисуем обычные уровни цен
        priceLevels.forEach { price ->
            val y = priceToY(price, priceRange, priceScaleArea.height)

            // Пропускаем текущую цену - ее нарисуем отдельно
            val isCurrentPrice = currentPrice != null &&
                    abs(price - currentPrice) / priceRange.range < 0.001

            if (!isCurrentPrice) {
                drawPriceLevel(
                    price = price,
                    y = y,
                    config = config,
                    priceScaleWidth = priceScaleArea.width,
                    textMeasurer = textMeasurer
                )
            }
        }

        // Затем рисуем badge текущей цены поверх всех остальных
        if (currentPrice != null) {
            val y = priceToY(currentPrice, priceRange, priceScaleArea.height)
            drawCurrentPriceBadge(
                price = currentPrice,
                y = y,
                priceScaleWidth = priceScaleArea.width,
                textMeasurer = textMeasurer,
                config = config
            )
        }
    }
}
private fun DrawScope.drawCurrentPriceBadge(
    price: Float,
    y: Float,
    priceScaleWidth: Float,
    textMeasurer: TextMeasurer,
    config: ChartConfig
) {
    // Текст цены
    val priceText = formatPrice(price)

    // Создаем стиль текста для badge
    val textStyle = androidx.compose.ui.text.TextStyle(
        color = Color.Green,
        fontSize = 10.sp,
        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
    )

    val textLayoutResult = textMeasurer.measure(
        text = androidx.compose.ui.text.AnnotatedString(priceText),
        style = textStyle
    )

    val textWidth = textLayoutResult.size.width
    val textHeight = textLayoutResult.size.height

    // Размеры badge с паддингами
    val padding = 4f
    val badgeWidth = textWidth + padding * 2
    val badgeHeight = textHeight + padding * 2

    // Позиция badge - выравниваем по правому краю шкалы
    val badgeLeft = priceScaleWidth - badgeWidth
    val badgeTop = y - badgeHeight / 2

    // Проверяем, чтобы badge не выходил за границы шкалы
    val adjustedBadgeTop = when {
        badgeTop < 0f -> 0f
        badgeTop + badgeHeight > size.height -> size.height - badgeHeight
        else -> badgeTop
    }

    // Рисуем фон badge
    drawRect(
        color = Color.Green.copy(alpha = 0.2f),
        topLeft = Offset(badgeLeft, adjustedBadgeTop),
        size = Size(badgeWidth, badgeHeight)
    )

    // Рисуем текст внутри badge
    drawText(
        textLayoutResult = textLayoutResult,
        topLeft = Offset(badgeLeft + padding, adjustedBadgeTop + padding)
    )

//    // Маленький зеленый кружок слева от badge
//    drawCircle(
//        color = Color.Green,
//        center = Offset(badgeLeft - 4f, adjustedBadgeTop + badgeHeight / 2),
//        radius = 2.5f
//    )
}
private fun DrawScope.drawTimeScale(
    candles: List<Candle>,
    config: ChartConfig,
    timeScaleArea: Rect,
    textMeasurer: TextMeasurer
) {
    if (candles.isEmpty()) return

    withTransform({
        translate(left = timeScaleArea.left, top = timeScaleArea.top)
        clipRect(0f, 0f, timeScaleArea.width, timeScaleArea.height)
    }) {
        // Фон шкалы времени
        drawRect(
            color = config.backgroundColor,
            topLeft = Offset(0f, 0f),
            size = Size(timeScaleArea.width, timeScaleArea.height)
        )

        // Разделительная линия сверху
        drawLine(
            color = config.gridColor.copy(alpha = 0.5f),
            start = Offset(0f, 0f),
            end = Offset(timeScaleArea.width, 0f),
            strokeWidth = 1f
        )

        // Рассчитываем метрики свечей для правильного позиционирования меток времени
        val candleMetrics = calculateCandleMetrics(candles.size, timeScaleArea.width)

        // Выбираем несколько свечей для отображения времени (чтобы не было слишком много меток)
        val step = (candles.size / 5).coerceAtLeast(1) // Показываем примерно 5 меток

        candles.forEachIndexed { index, candle ->
            // Показываем метку времени только для каждого step-ного элемента
            if (index % step == 0 || index == candles.size - 1) {
                val x = index * (candleMetrics.width + candleMetrics.spacing) + candleMetrics.width / 2

                // Форматируем время
                val timeText = formatTime(candle.timestamp)

                // Стиль текста для шкалы времени
                val textStyle = androidx.compose.ui.text.TextStyle(
                    color = config.axisTextColor,
                    fontSize = 9.sp,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                )

                val textLayoutResult = textMeasurer.measure(
                    text = androidx.compose.ui.text.AnnotatedString(timeText),
                    style = textStyle
                )

                // Рисуем вертикальную черточку
                drawLine(
                    color = config.gridColor,
                    start = Offset(x, 0f),
                    end = Offset(x, 4f),
                    strokeWidth = 1f
                )

                // Рисуем текст времени
                drawText(
                    textLayoutResult = textLayoutResult,
                    topLeft = Offset(
                        x - textLayoutResult.size.width / 2,
                        timeScaleArea.height - textLayoutResult.size.height - 2f
                    )
                )
            }
        }
    }
}
// Функция для рисования лейбла текущей цены
private fun DrawScope.drawCurrentPriceLabel(
    price: Float,
    y: Float,
    priceScaleWidth: Float,
    textMeasurer: TextMeasurer,
    config: ChartConfig
) {
    // Текст цены
    val priceText = formatPrice(price)

    // Создаем стиль текста
    val textStyle = androidx.compose.ui.text.TextStyle(
        color = Color.Green,
        fontSize = 10.sp,
        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
    )

    val textLayoutResult = textMeasurer.measure(
        text = androidx.compose.ui.text.AnnotatedString(priceText),
        style = textStyle
    )

    val textWidth = textLayoutResult.size.width
    val textHeight = textLayoutResult.size.height

    // Фон для лейбла
    val padding = 4f
    val rectLeft = priceScaleWidth - textWidth - padding * 2
    val rectTop = y - textHeight / 2 - padding
    val rectRight = priceScaleWidth
    val rectBottom = y + textHeight / 2 + padding

    // Рисуем фон
    drawRect(
        color = Color.Green.copy(alpha = 0.2f),
        topLeft = Offset(rectLeft, rectTop),
        size = Size(rectRight - rectLeft, rectBottom - rectTop)
    )

    // Рисуем текст
    drawText(
        textLayoutResult = textLayoutResult,
        topLeft = Offset(priceScaleWidth - textWidth - padding, y - textHeight / 2)
    )

    // Зеленая точка слева
    drawCircle(
        color = Color.Green,
        center = Offset(rectLeft - 6f, y),
        radius = 2.5f
    )
}

// Функция для рисования обычного уровня цены
private fun DrawScope.drawPriceLevel(
    price: Float,
    y: Float,
    config: ChartConfig,
    priceScaleWidth: Float,
    textMeasurer: TextMeasurer
) {
    val priceText = formatPrice(price)
    val textStyle = androidx.compose.ui.text.TextStyle(
        color = config.axisTextColor,
        fontSize = 10.sp,
        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
        textAlign = TextAlign.Right
    )

    val textLayoutResult = textMeasurer.measure(
        text = androidx.compose.ui.text.AnnotatedString(priceText),
        style = textStyle
    )

    drawText(
        textLayoutResult = textLayoutResult,
        topLeft = Offset(priceScaleWidth - textLayoutResult.size.width - 4f,
            y - textLayoutResult.size.height / 2)
    )
}
private fun calculatePriceRangeWithCurrentPrice(
    candles: List<Candle>,
    currentPrice: Float?
): PriceRange {
    if (candles.isEmpty() && currentPrice == null) {
        return PriceRange(0f, 0f, 0f, 0f, 0f)
    }

    val priceList = mutableListOf<Float>().apply {
        addAll(candles.map { it.high })
        addAll(candles.map { it.low })
        currentPrice?.let { add(it) }
    }

    val maxPrice = priceList.maxOrNull() ?: 0f
    val minPrice = priceList.minOrNull() ?: 0f
    val priceRange = maxPrice - minPrice

    // Добавляем 5% padding сверху и снизу
    val padding = priceRange * 0.05f
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

// Функция для конвертации цены в Y координату с учетом высоты области
private fun DrawScope.priceToY(price: Float, priceRange: PriceRange, height: Float): Float {
    return height - ((price - priceRange.visibleMin) / priceRange.range) * height
}

// Функция для отрисовки линии текущей цены
private fun DrawScope.drawCurrentPriceLine(
    currentPrice: Float,
    priceRange: PriceRange,
    config: ChartConfig,
    chartHeight: Float,
    chartWidth: Float
) {
    val y = priceToY(currentPrice, priceRange, chartHeight)

    // Пунктирная линия через весь график
    drawLine(
        color = Color.Green.copy(alpha = 0.7f),
        start = Offset(0f, y),
        end = Offset(chartWidth, y),
        strokeWidth = 1f,
        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 5f), 0f)
    )
}

// Функция для расчета метрик свечей
private fun calculateCandleMetrics(candleCount: Int, availableWidth: Float): CandleMetrics {
    val totalWidth = availableWidth / candleCount
    val width = totalWidth * 0.7f
    val spacing = totalWidth * 0.3f

    return CandleMetrics(width, spacing)
}

// Функция для рисования сетки
private fun DrawScope.drawGrid(
    config: ChartConfig,
    width: Float,
    height: Float
) {
    if (!config.showGrid) return

    // Горизонтальные линии
    val horizontalLines = 5
    for (i in 0..horizontalLines) {
        val y = height * i / horizontalLines.toFloat()

        drawLine(
            color = config.gridColor,
            start = Offset(0f, y),
            end = Offset(width, y),
            strokeWidth = 1f
        )
    }

    // Вертикальные линии
    val verticalLines = 10
    for (i in 0..verticalLines) {
        val x = width * i / verticalLines.toFloat()

        drawLine(
            color = config.gridColor,
            start = Offset(x, 0f),
            end = Offset(x, height),
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
    config: ChartConfig,
    chartHeight: Float
) {
    val style = config.candleStyle

    // Функция для конвертации цены в Y координату
    fun priceToYLocal(price: Float): Float {
        return priceToY(price, priceRange, chartHeight)
    }

    val isBullish = candle.close >= candle.open
    val bodyColor = if (isBullish) style.bullishColor else style.bearishColor
    val shadowColor = style.shadowColor

    // Координаты для отрисовки
    val openY = priceToYLocal(candle.open)
    val closeY = priceToYLocal(candle.close)
    val highY = priceToYLocal(candle.high)
    val lowY = priceToYLocal(candle.low)

    // 1. Рисуем верхнюю тень
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

        // 2. Рисуем нижнюю тень
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
            size = Size(metrics.width, bodyHeight)
        )
    } else {
        // Для Doji свечей рисуем линию
        drawLine(
            color = bodyColor,
            start = Offset(centerX - metrics.width / 2, bodyTop),
            end = Offset(centerX + metrics.width / 2, bodyTop),
            strokeWidth = 2f
        )
    }
}

// Функция для генерации уровней цен
private fun generatePriceLevels(min: Float, max: Float, count: Int): List<Float> {
    if (count <= 0) return emptyList()
    if (count == 1) return listOf(max)

    val range = max - min
    val step = range / (count - 1)

    return List(count) { i ->
        max - (step * i)
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
private fun formatTime(timestamp: Long): String {
    // Можно использовать разные форматы в зависимости от таймфрейма
    // Пока используем простое преобразование
    val date = java.util.Date(timestamp)
    val formatter = java.text.SimpleDateFormat("HH:mm")
    return formatter.format(date)
}