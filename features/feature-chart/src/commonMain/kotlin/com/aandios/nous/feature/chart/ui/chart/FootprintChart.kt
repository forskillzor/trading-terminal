package com.aandios.nous.feature.chart.ui.chart

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aandios.nous.api.market.model.FootprintCandle
import com.aandios.nous.feature.chart.model.ChartLayout
import com.aandios.nous.feature.chart.rendering.drawFootprintChart
import com.aandios.nous.feature.chart.rendering.drawPriceScale
import com.aandios.nous.feature.chart.ui.ChartConfig
import com.aandios.nous.feature.chart.ui.DefaultChartConfig
import com.aandios.nous.feature.chart.utils.calculateCandleMetrics
import com.aandios.nous.feature.chart.utils.calculatePriceRangeWithFootprint
import kotlin.math.max

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
    var chartWidthPx by remember { mutableFloatStateOf(0f) }
    var maxScroll by remember { mutableFloatStateOf(0f) }
    val maxScrollLeft = 300f

    val textMeasurer = rememberTextMeasurer()

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDrag = { change, _ ->
                        scrollOffset = (scrollOffset - change.position.x + change.previousPosition.x)
                            .coerceIn(-maxScrollLeft, maxScroll)
                    },
                )
            }
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
                            val rightEdge = scrollOffset + chartWidthPx
                            val newScrollOffset = rightEdge * actualFactor - chartWidthPx
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
        val priceRange = remember(visibleCandles) {
            calculatePriceRangeWithFootprint(visibleCandles)
        }

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .clipToBounds()
        ) {
            drawFootprintChart(
                candles = candles,
                priceRange = priceRange,
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
                    priceRange = priceRange,
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
