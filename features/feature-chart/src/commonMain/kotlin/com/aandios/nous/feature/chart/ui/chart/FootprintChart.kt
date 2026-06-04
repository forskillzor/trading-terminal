package com.aandios.nous.feature.chart.ui.chart

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.*
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
import androidx.compose.ui.unit.sp
import com.aandios.nous.api.market.model.FootprintCandle
import com.aandios.nous.feature.chart.model.ChartLayout
import com.aandios.nous.feature.chart.model.PriceRange
import com.aandios.nous.feature.chart.rendering.drawFootprintChart
import com.aandios.nous.feature.chart.rendering.drawPriceScale
import com.aandios.nous.feature.chart.ui.ChartConfig
import com.aandios.nous.feature.chart.ui.DefaultChartConfig
import com.aandios.nous.feature.chart.utils.calculateCandleMetrics
import com.aandios.nous.feature.chart.utils.calculatePriceRangeWithFootprint
import kotlin.math.max
import kotlin.math.min

@Composable
fun FootprintChart(
    candles: List<FootprintCandle>,
    modifier: Modifier = Modifier,
    config: ChartConfig = DefaultChartConfig,
    showPriceScale: Boolean = true,
    priceScaleWidth: Dp = 60.dp,
) {
    if (candles.isEmpty()) {
        BoxWithConstraints(modifier = modifier.fillMaxSize()) {
            Text(
                text = "No footprint data available.",
                color = androidx.compose.ui.graphics.Color.Gray,
                fontSize = 12.sp
            )
        }
        return
    }

    var scrollOffset by remember { mutableFloatStateOf(0f) }
    var zoomLevel by remember { mutableFloatStateOf(1f) }
    var verticalScroll by remember { mutableFloatStateOf(0f) }
    var chartWidthPx by remember { mutableFloatStateOf(0f) }
    var chartHeightPx by remember { mutableFloatStateOf(0f) }
    var maxScroll by remember { mutableFloatStateOf(0f) }
    var isCtrlPressed by remember { mutableStateOf(false) }
    var isAltPressed by remember { mutableStateOf(false) }
    val maxScrollLeft = 300f
    val maxZoom = 10f
    val minZoom = 0.15f
    val zoomStep = 1.25f

    val textMeasurer = rememberTextMeasurer()

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { }
            .onKeyEvent { event ->
                when {
                    event.key == Key.CtrlLeft || event.key == Key.CtrlRight -> {
                        isCtrlPressed = event.type == KeyEventType.KeyDown
                        true
                    }
                    event.key == Key.AltLeft || event.key == Key.AltRight -> {
                        isAltPressed = event.type == KeyEventType.KeyDown
                        true
                    }
                    else -> false
                }
            }
            .pointerInput(isAltPressed) {
                detectDragGestures(
                    onDrag = { change, dragAmount ->
                        if (isAltPressed) {
                            verticalScroll = (verticalScroll + dragAmount.y)
                                .coerceIn(-chartHeightPx * 2, chartHeightPx * 2)
                        } else {
                            scrollOffset = (scrollOffset - change.position.x + change.previousPosition.x)
                                .coerceIn(-maxScrollLeft, maxScroll)
                        }
                    },
                )
            }
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = {
                        zoomLevel = 1f
                        scrollOffset = maxScroll
                        verticalScroll = 0f
                    }
                )
            }
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull() ?: continue
                        val sd = change.scrollDelta
                        if (event.type == PointerEventType.Scroll && sd != Offset.Zero) {
                            val factor = if (sd.y < 0) zoomStep else 1f / zoomStep
                            val oldZoom = zoomLevel
                            val newZoom = (oldZoom * factor).coerceIn(minZoom, maxZoom)
                            val actualFactor = newZoom / oldZoom

                            val newScrollOffset = if (isCtrlPressed) {
                                val mouseX = change.position.x
                                val virtualPos = mouseX + scrollOffset
                                virtualPos * actualFactor - mouseX
                            } else {
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
        val canvasWidth = maxWidth
        val canvasHeight = maxHeight
        val density = LocalDensity.current

        val layout = remember(priceScaleWidth, canvasWidth, canvasHeight) {
            val widthPx = with(density) { canvasWidth.toPx() }
            val heightPx = with(density) { canvasHeight.toPx() }
            val chartPadding = 8f
            val timeScaleHeight = (heightPx * 0.04f).coerceAtLeast(20f).coerceAtMost(40f)
            val priceScaleWidthPx = with(density) { priceScaleWidth.toPx() }

            val priceScaleArea = Rect(
                left = widthPx - priceScaleWidthPx,
                top = 0f,
                right = widthPx,
                bottom = heightPx
            )
            val timeScaleArea = Rect(
                left = 0f,
                top = heightPx - timeScaleHeight,
                right = widthPx - priceScaleWidthPx - chartPadding,
                bottom = heightPx
            )
            val chartMainArea = Rect(
                left = 0f,
                top = 0f,
                right = widthPx - priceScaleWidthPx - chartPadding,
                bottom = heightPx - timeScaleHeight
            )
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

        chartWidthPx = layout.chartMainArea.width
        chartHeightPx = layout.chartMainArea.height
        val candleMetrics = remember(zoomLevel) {
            calculateCandleMetrics(zoomLevel)
        }
        val totalW = candleMetrics.width + candleMetrics.spacing
        maxScroll = max(0f, candles.size * totalW - chartWidthPx)

        LaunchedEffect(candles.firstOrNull()?.startTime ?: 0L) {
            scrollOffset = maxScroll
        }

        val clampedOffset = scrollOffset.coerceIn(-maxScrollLeft, maxScroll)
        val startIdx = (clampedOffset / totalW).toInt().coerceIn(0, max(0, candles.size - 1))
        val endIdx = ((clampedOffset + chartWidthPx) / totalW + 1).toInt().coerceIn(startIdx + 1, candles.size)

        val visibleCandles = remember(startIdx, endIdx) {
            candles.subList(startIdx, endIdx.coerceAtMost(candles.size))
        }
        val basePriceRange = remember(visibleCandles) {
            calculatePriceRangeWithFootprint(visibleCandles)
        }
        val shiftedPriceRange = remember(basePriceRange, verticalScroll, chartHeightPx) {
            val shiftRatio = verticalScroll / chartHeightPx.coerceAtLeast(1f)
            val shift = basePriceRange.range * shiftRatio
            PriceRange(
                max = basePriceRange.max + shift,
                min = basePriceRange.min + shift,
                visibleMax = basePriceRange.visibleMax + shift,
                visibleMin = basePriceRange.visibleMin + shift,
                range = basePriceRange.range
            )
        }

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .clipToBounds()
        ) {
            drawFootprintChart(
                candles = candles,
                priceRange = shiftedPriceRange,
                config = config,
                chartArea = layout.chartArea,
                textMeasurer = textMeasurer,
                scrollOffset = clampedOffset,
                zoomLevel = zoomLevel,
                visibleStartIndex = startIdx,
                visibleEndIndex = endIdx,
            )

            if (showPriceScale) {
                drawPriceScale(
                    priceRange = shiftedPriceRange,
                    config = config,
                    priceScaleArea = layout.priceScaleArea,
                    currentPrice = null,
                    textMeasurer = textMeasurer
                )
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
