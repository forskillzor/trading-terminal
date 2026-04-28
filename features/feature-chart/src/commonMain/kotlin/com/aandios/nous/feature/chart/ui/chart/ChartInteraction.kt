package com.aandios.nous.feature.chart.ui.chart

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.aandios.nous.api.market.model.Candle
import com.aandios.nous.feature.chart.model.CandleMetrics
import com.aandios.nous.feature.chart.model.ChartLayout
import com.aandios.nous.feature.chart.model.PriceRange
import com.aandios.nous.feature.chart.rendering.drawChart
import com.aandios.nous.feature.chart.rendering.drawCrosshair
import com.aandios.nous.feature.chart.rendering.drawPriceScale
import com.aandios.nous.feature.chart.rendering.drawTimeScale
import com.aandios.nous.feature.chart.ui.ChartConfig
import com.aandios.nous.feature.chart.ui.DefaultChartConfig
import com.aandios.nous.feature.chart.utils.calculateCandleMetrics
import com.aandios.nous.feature.chart.utils.calculatePriceRangeWithCurrentPrice
import kotlin.math.max

/**
 * Композабл, управляющий интерактивным поведением графика: скролл, зум, crosshair,
 * ленивая загрузка истории, расчёт layout и рендеринг холста.
 *
 * Выделен из CandleStickChart для соблюдения SRP — сам график становится тонкой
 * обёрткой, а вся сложная логика взаимодействия находится здесь.
 */
@Composable
fun CandleStickChartInteraction(
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
    // Ширина области графика в пикселях — обновляется внутри BoxWithConstraints, нужна для зума
    var chartWidthPx by remember { mutableFloatStateOf(0f) }
    // Максимальный скролл (полная ширина свечей минус ширина области графика) — ограничивает скролл справа
    var maxScroll by remember { mutableFloatStateOf(0f) }
    var isCtrlPressed by remember { mutableStateOf(false) }
    // Максимальное пустое место слева (в пикселях) — триггер для загрузки истории
    val maxScrollLeft = 300f

    // TextMeasurer для измерения текста
    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { /* no-op: make composable focusable for onKeyEvent */ }
            .onKeyEvent { event ->
                if (event.key == Key.CtrlLeft || event.key == Key.CtrlRight) {
                    isCtrlPressed = event.type == KeyEventType.KeyDown
                    true
                } else {
                    false
                }
            }
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
                            scrollOffset = (scrollOffset - deltaX).coerceIn(-maxScrollLeft, maxScroll)
                        },
                    )
                }
            }
            // Зум: без Ctrl — от правого края, с Ctrl — от свечи под курсором
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull() ?: continue
                        val sd = change.scrollDelta
                        if (event.type == PointerEventType.Scroll && sd != Offset.Zero) {
                            val factor = if (sd.y < 0) 1.15f else 1f / 1.15f
                            val oldZoom = zoomLevel
                            val newZoom = (oldZoom * factor).coerceIn(0.25f, 4.0f)
                            val actualFactor = newZoom / oldZoom

                            val mouseX = change.position.x
                            val newScrollOffset = if (isCtrlPressed) {
                                // Ctrl+zoom: фиксируем свечу под курсором
                                val virtualPos = mouseX + scrollOffset
                                virtualPos * actualFactor - mouseX
                            } else {
                                // Обычный зум: фиксируем правый край (самую новую свечу)
                                val rightEdge = scrollOffset + chartWidthPx
                                rightEdge * actualFactor - chartWidthPx
                            }

                            zoomLevel = newZoom
                            scrollOffset = newScrollOffset

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

        val layout = remember(priceScaleWidth, canvasWidth, canvasHeight) {
            val widthPx = with(density) { canvasWidth.toPx() }
            val heightPx = with(density) { canvasHeight.toPx() }
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
        chartWidthPx = layout.chartMainArea.width
        val candleMetrics = remember(zoomLevel) {
            calculateCandleMetrics(zoomLevel)
        }
        val totalW = candleMetrics.width + candleMetrics.spacing
        maxScroll = max(0f, candles.size * totalW - chartWidthPx)

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

        // PriceRange только по видимым свечам (Y-масштаб адаптируется при зум/скролле)
        val visibleCandles = remember(startIdx, endIdx) {
            candles.subList(startIdx, endIdx.coerceAtMost(candles.size))
        }
        val priceRange = remember(visibleCandles, currentPrice) {
            calculatePriceRangeWithCurrentPrice(visibleCandles, currentPrice)
        }

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
                scrollOffset = scrollOffset.coerceIn(-maxScrollLeft, maxScroll)
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
