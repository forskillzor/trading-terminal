package com.aandios.nous.feature.chart.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aandios.nous.api.market.model.Candle
import com.aandios.nous.feature.chart.utils.formatPrice
import com.aandios.nous.feature.chart.utils.formatTime
import kotlin.math.abs
import kotlin.math.max


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
    priceScaleWidth: Dp = 60.dp,
    crosshairEnabled: Boolean = false,
    onCrosshairEnabledChange: (Boolean) -> Unit = {},
    onNeedMoreHistory: () -> Unit = {},
    historyLoadCount: Int = 0,
    hasMoreHistory: Boolean = true,
) {
    if (candles.isEmpty()) return

    var mousePosition by remember { mutableStateOf<Offset?>(null) }
    var isCrosshairVisible by remember { mutableStateOf(false) }
    var scrollOffset by remember { mutableFloatStateOf(0f) }
    var zoomLevel by remember { mutableFloatStateOf(1f) }
    // Максимальное пустое место слева (в пикселях) — триггер для загрузки истории
    val maxScrollLeft = 300f

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
            // Обработка жестов: pan (crosshair off) или crosshair (crosshair on)
            .pointerInput(crosshairEnabled) {
                if (crosshairEnabled) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        isCrosshairVisible = true
                        mousePosition = down.position
                        do {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull() ?: break
                            if (change.pressed) {
                                isCrosshairVisible = true
                                mousePosition = change.position
                                change.consume()
                            } else {
                                change.consume()
                                break
                            }
                        } while (true)
                    }
                } else {
                    detectDragGestures(
                        onDrag = { change, _ ->
                            val deltaX = change.position.x - change.previousPosition.x
                            scrollOffset = (scrollOffset - deltaX).coerceIn(-maxScrollLeft, Float.MAX_VALUE)
                        },
                    )
                }
            }
            // Зум колесиком мыши — всегда относительно свечи под курсором
            .pointerInput(candles.size) {
                val pxPriceScaleWidth = with(density) { priceScaleWidth.toPx() }
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull() ?: continue
                        val sd = change.scrollDelta
                        if (event.type == PointerEventType.Scroll && sd != Offset.Zero) {
                            val factor = if (sd.y < 0) 1.15f else 1f / 1.15f
                            val oldZoom = zoomLevel
                            val newZoom = (oldZoom * factor).coerceIn(0.3f, 5.0f)
                            val actualFactor = newZoom / oldZoom
                            val mouseX = change.position.x

                            // Зум относительно свечи под курсором
                            val virtualPos = mouseX + scrollOffset
                            val newScrollOffset = virtualPos * actualFactor - mouseX
                            zoomLevel = newZoom
                            scrollOffset = newScrollOffset.coerceIn(-maxScrollLeft, Float.MAX_VALUE)

                            change.consume()
                        }
                    }
                }
            }

    ) {
        // Рассчитываем layout графика
        val canvasWidth = maxWidth
        val canvasHeight = maxHeight

        val density = LocalDensity.current

        // Рассчитываем layout графика
        val layout = remember(priceScaleWidth, canvasWidth, canvasHeight) {
            val widthPx = with(density){ canvasWidth.toPx()}
            val heightPx = with(density){ canvasHeight.toPx()}
            val chartPadding = 8f

            // Динамическая высота шкалы времени
            val timeScaleHeight = (heightPx * 0.04f).coerceAtLeast(20f).coerceAtMost(40f)

            val priceScaleWidthPx = with(density) {
                priceScaleWidth.toPx()
            }

            // Область для шкалы цен
            val priceScaleArea = Rect(
                left = widthPx - priceScaleWidthPx,
                top = 0f,
                right = widthPx,
                bottom = heightPx
            )

            // Область для шкалы времени (внизу)
            val timeScaleArea = Rect(
                left = 0f,
                top = heightPx - timeScaleHeight,
                right = widthPx - priceScaleWidthPx - chartPadding,
                bottom = heightPx
            )

            // Основная область графика (без шкалы времени)
            val chartMainArea = Rect(
                left = 0f,
                top = 0f,
                right = widthPx - priceScaleWidthPx - chartPadding,
                bottom = heightPx - timeScaleHeight
            )

            // Вся область графика (включая шкалу времени)
            val chartArea = Rect(
                left = 0f,
                top = 0f,
                right = widthPx - priceScaleWidthPx - chartPadding,
                bottom = heightPx
            )

            ChartLayout(
                canvasWidth = widthPx,
                canvasHeight = heightPx,
                priceScaleWidth = priceScaleWidthPx,
                chartArea = chartArea,
                priceScaleArea = priceScaleArea,
                chartPadding = chartPadding,
                timeScaleHeight = timeScaleHeight,
                chartMainArea = chartMainArea,
                timeScaleArea = timeScaleArea
            )
        }

        // Расчет метрик свечей и скролла
        val chartWidthPx = layout.chartMainArea.width
        val candleMetrics = remember(candles.size, chartWidthPx, zoomLevel) {
            calculateCandleMetrics(candles.size, chartWidthPx * zoomLevel)
        }
        val totalW = candleMetrics.width + candleMetrics.spacing
        val maxScroll = max(0f, chartWidthPx * zoomLevel - chartWidthPx)

        // При загрузке новых данных (смена символа/таймфрейма) показываем последние свечи
        // НЕ срабатывает при prepend исторических свечей (historyLoadCount > 0)
        LaunchedEffect(candles.firstOrNull()?.timestamp ?: 0L) {
            if (historyLoadCount == 0) {
                scrollOffset = maxScroll
            }
        }

        // Клиппинг scrollOffset
        val clampedOffset = scrollOffset.coerceIn(-maxScrollLeft, maxScroll)

        // Вычисление видимого диапазона свечей
        val startIdx = (clampedOffset / totalW).toInt().coerceIn(0, max(0, candles.size - 1))
        val endIdx = ((clampedOffset + chartWidthPx) / totalW + 1).toInt().coerceIn(startIdx + 1, candles.size)

        // Lazy loading historical candles: когда пользователь скроллит левее первой свечи
        // (clampedOffset < 0) — появляется пустое место, вызываем загрузку истории
        LaunchedEffect(clampedOffset, hasMoreHistory) {
            println("[DEBUG-WIDGET] >>> LaunchedEffect fired: clampedOffset=$clampedOffset, hasMoreHistory=$hasMoreHistory, candles.size=${candles.size}, scrollOffset=$scrollOffset")
            if (hasMoreHistory && clampedOffset < 0f) {
                println("[DEBUG-WIDGET] >>> TRIGGERING onNeedMoreHistory()")
                onNeedMoreHistory()
            }
        }

        // Коррекция scrollOffset после загрузки исторических свечей
        LaunchedEffect(historyLoadCount, candles.size) {
            println("[DEBUG-WIDGET] >>> LaunchedEffect(historyLoadCount=$historyLoadCount, candleCount=${candles.size})")
            if (historyLoadCount > 0) {
                val oldScrollOffset = scrollOffset
                val added = historyLoadCount * totalW
                scrollOffset += added
                println("[DEBUG-WIDGET] Corrected scrollOffset: $oldScrollOffset -> $scrollOffset (added $added, totalW=$totalW, candles.size=${candles.size})")
            }
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
                textMeasurer = textMeasurer,
                scrollOffset = clampedOffset,
                zoomLevel = zoomLevel,
                visibleStartIndex = startIdx,
                visibleEndIndex = endIdx,
            )

            drawTimeScale(
                candles = candles,
                config = config,
                timeScaleArea = layout.timeScaleArea,
                textMeasurer = textMeasurer,
                scrollOffset = clampedOffset,
                zoomLevel = zoomLevel,
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
            // Рисуем перекрестие если crosshair включен и есть позиция курсора
            if (crosshairEnabled && isCrosshairVisible && mousePosition != null) {
                drawCrosshair(
                    mousePosition = mousePosition!!,
                    candles = candles,
                    priceRange = priceRange,
                    config = config,
                    chartLayout = layout,
                    textMeasurer = textMeasurer,
                    scrollOffset = clampedOffset,
                    zoomLevel = zoomLevel,
                )
            }
        }
    }
}

// Функция для отрисовки перекрестия
private fun DrawScope.drawCrosshair(
    mousePosition: Offset,
    candles: List<Candle>,
    priceRange: PriceRange,
    config: ChartConfig,
    chartLayout: ChartLayout,
    textMeasurer: TextMeasurer,
    scrollOffset: Float = 0f,
    zoomLevel: Float = 1f,
) {
    // Проверяем находится ли курсор в области графика (без шкалы времени)
    if (mousePosition.x < chartLayout.chartMainArea.left ||
        mousePosition.x > chartLayout.chartMainArea.right ||
        mousePosition.y < chartLayout.chartMainArea.top ||
        mousePosition.y > chartLayout.chartMainArea.bottom) {
        return // Курсор вне области графика
    }

    // 1. Вертикальная линия через весь график
    drawLine(
        color = Color.White.copy(alpha = 0.3f),
        start = Offset(mousePosition.x, chartLayout.chartMainArea.top),
        end = Offset(mousePosition.x, chartLayout.chartMainArea.bottom),
        strokeWidth = 1f
    )

    drawLine(
        color = Color.White.copy(alpha = 0.3f),
        start = Offset(chartLayout.chartMainArea.left, mousePosition.y),
        end = Offset(chartLayout.chartMainArea.right, mousePosition.y),
        strokeWidth = 1f
    )

    // 3. Находим ближайшую свечу к позиции курсора по X
    val candleIndex = findNearestCandleIndex(
        mouseX = mousePosition.x,
        candles = candles,
        chartWidth = chartLayout.chartMainArea.width,
        scrollOffset = scrollOffset,
        zoomLevel = zoomLevel,
    )

    // 4. Если нашли свечу, показываем информацию о ней
    if (candleIndex in candles.indices) {
        val candle = candles[candleIndex]

        // Рассчитываем Y для цен свечи
        fun getYForPrice(price: Float): Float {
            return chartLayout.chartMainArea.height -
                    ((price - priceRange.visibleMin) / priceRange.range) *
                    chartLayout.chartMainArea.height
        }

        // 5. Маленькие маркеры на уровнях цен свечи
        val highY = getYForPrice(candle.high)
        val lowY = getYForPrice(candle.low)
        val openY = getYForPrice(candle.open)
        val closeY = getYForPrice(candle.close)

        // Маркер на high
        drawCircle(
            color = Color.Red.copy(alpha = 0.7f),
            center = Offset(mousePosition.x, highY),
            radius = 3f
        )

        // Маркер на low
        drawCircle(
            color = Color.Green.copy(alpha = 0.7f),
            center = Offset(mousePosition.x, lowY),
            radius = 3f
        )

        // 6. Информационная панель в углу
        drawInfoPanel(
            candle = candle,
            mousePosition = mousePosition,
            chartLayout = chartLayout,
            textMeasurer = textMeasurer,
            config = config
        )

        // 7. Метка цены на оси Y
        val currentPriceAtCursor = priceFromY(
            y = mousePosition.y,
            priceRange = priceRange,
            chartHeight = chartLayout.chartMainArea.height
        )

        drawPriceLabelOnAxis(
            price = currentPriceAtCursor,
            mouseY = mousePosition.y,
            chartLayout = chartLayout,
            textMeasurer = textMeasurer,
            config = config
        )

        // 8. Метка времени на оси X
        drawTimeLabelOnAxis(
            candle = candle,
            mouseX = mousePosition.x,
            chartLayout = chartLayout,
            textMeasurer = textMeasurer,
            config = config
        )
    }
}
// Функция для поиска индекса ближайшей свечи по X координате
private fun findNearestCandleIndex(
    mouseX: Float,
    candles: List<Candle>,
    chartWidth: Float,
    scrollOffset: Float = 0f,
    zoomLevel: Float = 1f,
): Int {
    if (candles.isEmpty()) return -1

    val candleMetrics = calculateCandleMetrics(candles.size, chartWidth * zoomLevel)
    val totalWidthPerCandle = candleMetrics.width + candleMetrics.spacing

    // mouseX — координата на видимой области, свечи смещены на -scrollOffset в виртуальном пространстве
    val virtualX = mouseX + scrollOffset
    val index = (virtualX / totalWidthPerCandle).toInt()
    return index.coerceIn(0, candles.size - 1)
}

// Функция для получения цены из Y координаты
private fun priceFromY(
    y: Float,
    priceRange: PriceRange,
    chartHeight: Float
): Float {
    return priceRange.visibleMax - (y / chartHeight) * priceRange.range
}

// Функция для рисования информационной панели
private fun DrawScope.drawInfoPanel(
    candle: Candle,
    mousePosition: Offset,
    chartLayout: ChartLayout,
    textMeasurer: TextMeasurer,
    config: ChartConfig
) {
    val panelWidth = 120f
    val panelHeight = 80f

    // Позиция панели (правый верхний угол)
    val panelLeft = mousePosition.x + 10f
    val panelTop = mousePosition.y + 10f

    // Проверяем чтобы панель не выходила за границы
    val adjustedLeft = if (panelLeft + panelWidth > chartLayout.chartMainArea.right) {
        mousePosition.x - panelWidth - 10f
    } else {
        panelLeft
    }

    val adjustedTop = if (panelTop + panelHeight > chartLayout.chartMainArea.bottom) {
        mousePosition.y - panelHeight - 10f
    } else {
        panelTop
    }

    // Фон панели
    drawRect(
        color = Color.Black.copy(alpha = 0.8f),
        topLeft = Offset(adjustedLeft, adjustedTop),
        size = Size(panelWidth, panelHeight)
    )

    // Время свечи
    val timeText = "Time: ${formatTime(candle.timestamp)}"
    drawTextLine(
        text = timeText,
        x = adjustedLeft + 4f,
        y = adjustedTop + 15f,
        textMeasurer = textMeasurer,
        color = Color.White
    )

    // Цены
    drawTextLine(
        text = String.format("O: %.4f", candle.open),
        x = adjustedLeft + 4f,
        y = adjustedTop + 30f,
        textMeasurer = textMeasurer,
        color = Color.White
    )

    drawTextLine(
        text = String.format("H: %.4f", candle.high),
        x = adjustedLeft + 4f,
        y = adjustedTop + 45f,
        textMeasurer = textMeasurer,
        color = if (candle.high >= candle.open) Color.Green else Color.Red
    )

    drawTextLine(
        text = String.format("L: %.4f", candle.low),
        x = adjustedLeft + 4f,
        y = adjustedTop + 60f,
        textMeasurer = textMeasurer,
        color = if (candle.low <= candle.open) Color.Red else Color.Green
    )
}

// Функция для рисования метки цены на оси Y
private fun DrawScope.drawPriceLabelOnAxis(
    price: Float,
    mouseY: Float,
    chartLayout: ChartLayout,
    textMeasurer: TextMeasurer,
    config: ChartConfig
) {
    val priceText = String.format("%.4f", price)

    val textStyle = TextStyle(
        color = Color.White,
        fontSize = 10.sp,
        fontFamily = FontFamily.Monospace,
    )

    val textLayoutResult = textMeasurer.measure(
        text = androidx.compose.ui.text.AnnotatedString(priceText),
        style = textStyle
    )

    // Позиция на правой стороне графика
    val labelX = chartLayout.chartMainArea.right - textLayoutResult.size.width - 4f
    val labelY = mouseY - textLayoutResult.size.height / 2

    drawRect(
        color = Color.Black.copy(alpha = 0.7f),
        topLeft = Offset(labelX, labelY),
        size = Size(
            textLayoutResult.size.width.toFloat(),
            textLayoutResult.size.height.toFloat()
        )
    )

    drawText(
        textLayoutResult = textLayoutResult,
        topLeft = Offset(labelX, labelY)
    )
}

// Функция для рисования метки времени на оси X
private fun DrawScope.drawTimeLabelOnAxis(
    candle: Candle,
    mouseX: Float,
    chartLayout: ChartLayout,
    textMeasurer: TextMeasurer,
    config: ChartConfig
) {
    val timeText = formatTime(candle.timestamp)

    val textStyle = TextStyle(
        color = Color.White,
        fontSize = 10.sp,
        fontFamily = FontFamily.Monospace,
    )

    val textLayoutResult = textMeasurer.measure(
        text = AnnotatedString(timeText),
        style = textStyle
    )

    // Позиция внизу графика
    val labelX = mouseX - textLayoutResult.size.width / 2
    val labelY = chartLayout.chartMainArea.bottom + 4f

    // Проверяем границы
    val adjustedX = labelX.coerceIn(
        0f,
        chartLayout.chartMainArea.right - textLayoutResult.size.width
    )

    drawRect(
        color = Color.Black.copy(alpha = 0.7f),
        topLeft = Offset(adjustedX, labelY),
        size = Size(
            textLayoutResult.size.width.toFloat(),
            textLayoutResult.size.height.toFloat()
        )
    )

    drawText(
        textLayoutResult = textLayoutResult,
        topLeft = Offset(adjustedX, labelY)
    )
}

// Вспомогательная функция для рисования текста
private fun DrawScope.drawTextLine(
    text: String,
    x: Float,
    y: Float,
    textMeasurer: TextMeasurer,
    color: Color
) {
    val textStyle = TextStyle(
        color = color,
        fontSize = 10.sp,
        fontFamily = FontFamily.Monospace
    )

    val textLayoutResult = textMeasurer.measure(
        text = AnnotatedString(text),
        style = textStyle
    )

    drawText(
        textLayoutResult = textLayoutResult,
        topLeft = Offset(x, y)
    )
}
// Функция для отрисовки всего графика
private fun DrawScope.drawChart(
    candles: List<Candle>,
    priceRange: PriceRange,
    config: ChartConfig,
    chartArea: Rect,
    currentPrice: Float?,
    textMeasurer: TextMeasurer,
    scrollOffset: Float = 0f,
    zoomLevel: Float = 1f,
    visibleStartIndex: Int = 0,
    visibleEndIndex: Int = 0,
) {
    // Сохраняем область рисования для графика
    withTransform({
        translate(left = chartArea.left, top = chartArea.top)
        clipRect(0f, 0f, chartArea.width, chartArea.height)
    }) {
        // Сначала сетка
        drawGrid(config, chartArea.width, chartArea.height)

        // Потом свечи — только видимые
        val candleMetrics = calculateCandleMetrics(candles.size, chartArea.width * zoomLevel)
        val totalW = candleMetrics.width + candleMetrics.spacing
        for (i in visibleStartIndex until visibleEndIndex) {
            if (i in candles.indices) {
                val x = i * totalW - scrollOffset + candleMetrics.width / 2
                drawCandle(
                    candle = candles[i],
                    centerX = x,
                    priceRange = priceRange,
                    metrics = candleMetrics,
                    config = config,
                    chartHeight = chartArea.height
                )
            }
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
    val textStyle = TextStyle(
        color = Color.Green,
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold,
        fontFamily = FontFamily.Monospace
    )

    val textLayoutResult = textMeasurer.measure(
        text = AnnotatedString(priceText),
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
}

private fun DrawScope.drawTimeScale(
    candles: List<Candle>,
    config: ChartConfig,
    timeScaleArea: Rect,
    textMeasurer: TextMeasurer,
    scrollOffset: Float = 0f,
    zoomLevel: Float = 1f,
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
        val candleMetrics = calculateCandleMetrics(candles.size, timeScaleArea.width * zoomLevel)
        val totalW = candleMetrics.width + candleMetrics.spacing

        // Определяем видимый диапазон индексов по скроллу
        val visibleStartIdx = (scrollOffset / totalW).toInt().coerceIn(0, max(0, candles.size - 1))
        val visibleEndIdx = ((scrollOffset + timeScaleArea.width) / totalW + 1).toInt().coerceIn(0, candles.size)
        val visibleCount = visibleEndIdx - visibleStartIdx

        // Выбираем шаг меток — примерно 5–7 на видимую область
        val step = (visibleCount / 6).coerceAtLeast(1)

        // Показываем метку на первой видимой свече
        val firstLabelIdx = visibleStartIdx + (step - visibleStartIdx % step) % step

        for (i in firstLabelIdx until visibleEndIdx step step) {
            if (i in candles.indices) {
                val x = i * totalW - scrollOffset + candleMetrics.width / 2

                // Форматируем время
                val timeText = formatTime(candles[i].timestamp)

                // Стиль текста для шкалы времени
                val textStyle = TextStyle(
                    color = config.axisTextColor,
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace
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
    val textStyle = TextStyle(
        color = Color.Green,
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold,
        fontFamily = FontFamily.Monospace
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
    val textStyle = TextStyle(
        color = config.axisTextColor,
        fontSize = 10.sp,
        fontFamily = FontFamily.Monospace,
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
    val bodyHeight = abs(bodyBottom - bodyTop)

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