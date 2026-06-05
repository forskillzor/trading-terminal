package com.aandios.nous.feature.chart.rendering

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.sp
import com.aandios.nous.api.market.model.FootprintCandle
import com.aandios.nous.core.ui.theme.ChartColors
import com.aandios.nous.feature.chart.model.CandleMetrics
import com.aandios.nous.feature.chart.model.ChartLayout
import com.aandios.nous.feature.chart.model.PriceRange
import com.aandios.nous.feature.chart.ui.ChartConfig
import com.aandios.nous.feature.chart.utils.calculateCandleMetrics
import com.aandios.nous.feature.chart.utils.priceFromY
import com.aandios.nous.feature.chart.utils.priceToY
import com.aandios.nous.feature.chart.utils.formatTime
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

fun DrawScope.drawFootprintChart(
    candles: List<FootprintCandle>,
    priceRange: PriceRange,
    config: ChartConfig,
    chartArea: Rect,
    textMeasurer: TextMeasurer,
    scrollOffset: Float = 0f,
    zoomLevel: Float = 1f,
    visibleStartIndex: Int = 0,
    visibleEndIndex: Int = 0,
) {
    withTransform({
        translate(left = chartArea.left, top = chartArea.top)
        clipRect(0f, 0f, chartArea.width, chartArea.height)
    }) {
        drawGrid(config, chartArea.width, chartArea.height)

        val candleMetrics = calculateCandleMetrics(zoomLevel)
        val fpWidth = candleMetrics.width
        val fpSpacing = candleMetrics.spacing
        val totalW = fpWidth + fpSpacing

        // Viewport max volume for gradient normalization
        var viewportMaxVol = 0f
        for (i in visibleStartIndex until visibleEndIndex) {
            if (i in candles.indices) {
                val v = candles[i].maxVolume
                if (v > viewportMaxVol) viewportMaxVol = v
            }
        }
        if (viewportMaxVol <= 0f) viewportMaxVol = 1f

        for (i in visibleStartIndex until visibleEndIndex) {
            if (i in candles.indices) {
                val x = i * totalW - scrollOffset + fpWidth / 2
                drawFootprintCandle(
                    candle = candles[i],
                    centerX = x,
                    priceRange = priceRange,
                    metrics = CandleMetrics(fpWidth * 2f, fpSpacing), // 2x wider
                    config = config,
                    chartHeight = chartArea.height,
                    viewportMaxVol = viewportMaxVol
                )
            }
        }
    }
}

fun DrawScope.drawFootprintCandle(
    candle: FootprintCandle,
    centerX: Float,
    priceRange: PriceRange,
    metrics: CandleMetrics,
    config: ChartConfig,
    chartHeight: Float,
    viewportMaxVol: Float = 1f
) {
    if (candle.levels.isEmpty()) return

    val halfWidth = metrics.width / 2
    val maxBarWidth = halfWidth * 0.85f
    val minAlpha = 0.12f

    for (level in candle.levels) {
        val priceY = priceToY(level.priceFloat, priceRange, chartHeight)
        val nextPrice = if (candle.levels.indexOf(level) < candle.levels.size - 1) {
            candle.levels[candle.levels.indexOf(level) + 1].priceFloat
        } else {
            level.priceFloat - (candle.levels.getOrNull(candle.levels.size - 2)?.priceFloat?.let {
                candle.levels.last().priceFloat - it
            } ?: 1f)
        }
        val nextPriceY = priceToY(nextPrice, priceRange, chartHeight)
        val levelHeight = abs(nextPriceY - priceY).coerceAtLeast(1f)
        val topY = minOf(priceY, nextPriceY)

        val bidWidth = (level.bidVolumeFloat / viewportMaxVol * maxBarWidth).coerceAtLeast(if (level.bidVolumeFloat > 0f) 1f else 0f)
        val askWidth = (level.askVolumeFloat / viewportMaxVol * maxBarWidth).coerceAtLeast(if (level.askVolumeFloat > 0f) 1f else 0f)

        if (askWidth > 0f) {
            val alpha = (minAlpha + (1f - minAlpha) * (level.askVolumeFloat / viewportMaxVol)).coerceIn(minAlpha, 1f)
            drawRect(
                color = ChartColors.bearish.copy(alpha = alpha),
                topLeft = Offset(centerX - halfWidth + (maxBarWidth - askWidth), topY),
                size = Size(askWidth, levelHeight)
            )
        }
        if (bidWidth > 0f) {
            val alpha = (minAlpha + (1f - minAlpha) * (level.bidVolumeFloat / viewportMaxVol)).coerceIn(minAlpha, 1f)
            drawRect(
                color = ChartColors.bullish.copy(alpha = alpha),
                topLeft = Offset(centerX + halfWidth - maxBarWidth, topY),
                size = Size(bidWidth, levelHeight)
            )
        }
    }
}

fun DrawScope.drawTimeScaleForFootprint(
    candles: List<FootprintCandle>,
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
        drawRect(config.backgroundColor, Offset(0f, 0f), Size(timeScaleArea.width, timeScaleArea.height))
        drawLine(config.gridColor.copy(alpha = 0.5f), Offset(0f, 0f), Offset(timeScaleArea.width, 0f), 1f)

        val candleMetrics = calculateCandleMetrics(zoomLevel)
        val totalW = candleMetrics.width + candleMetrics.spacing

        val visibleStartIdx = (scrollOffset / totalW).toInt().coerceIn(0, max(0, candles.size - 1))
        val visibleEndIdx = ((scrollOffset + timeScaleArea.width) / totalW + 1).toInt().coerceIn(0, candles.size)
        val visibleCount = visibleEndIdx - visibleStartIdx
        val step = (visibleCount / 6).coerceAtLeast(1)
        val firstLabelIdx = visibleStartIdx + (step - visibleStartIdx % step) % step

        for (i in firstLabelIdx until visibleEndIdx step step) {
            if (i in candles.indices) {
                val x = i * totalW - scrollOffset + candleMetrics.width / 2
                val timeText = formatTime(candles[i].startTime)
                val textStyle = TextStyle(color = config.axisTextColor, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                val textLayoutResult = textMeasurer.measure(AnnotatedString(timeText), textStyle)
                drawLine(config.gridColor, Offset(x, 0f), Offset(x, 4f), 1f)
                drawText(textLayoutResult, topLeft = Offset(x - textLayoutResult.size.width / 2, timeScaleArea.height - textLayoutResult.size.height - 2f))
            }
        }
    }
}

fun DrawScope.drawCrosshairForFootprint(
    mousePosition: Offset,
    candles: List<FootprintCandle>,
    priceRange: PriceRange,
    config: ChartConfig,
    chartLayout: ChartLayout,
    textMeasurer: TextMeasurer,
    scrollOffset: Float = 0f,
    zoomLevel: Float = 1f,
) {
    val candleMetrics = calculateCandleMetrics(zoomLevel)
    val totalW = candleMetrics.width + candleMetrics.spacing
    val virtualX = mousePosition.x + scrollOffset
    val candleIndex = (virtualX / totalW).toInt().coerceIn(0, candles.size - 1)
    val candle = candles.getOrNull(candleIndex) ?: return
    val candleX = candleIndex * totalW - scrollOffset + candleMetrics.width / 2

    val crosshairColor = config.gridColor.copy(alpha = 0.8f)

    drawLine(color = crosshairColor, start = Offset(candleX, 0f), end = Offset(candleX, chartLayout.chartMainArea.height), strokeWidth = 1f)
    drawLine(color = crosshairColor, start = Offset(0f, mousePosition.y), end = Offset(chartLayout.chartMainArea.width, mousePosition.y), strokeWidth = 1f)

    val info = buildString {
        appendLine("O: ${candle.open}")
        appendLine("H: ${candle.high}")
        appendLine("L: ${candle.low}")
        appendLine("C: ${candle.close}")
        appendLine("Ticks: ${candle.totalTicks}")
    }
    val textStyle = TextStyle(color = config.axisTextColor, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
    val layoutResult = textMeasurer.measure(AnnotatedString(info), textStyle)

    val panelX = if (candleX + layoutResult.size.width + 20f > chartLayout.chartMainArea.width) candleX - layoutResult.size.width - 10f else candleX + 10f
    val panelY = 10f
    drawRect(color = config.backgroundColor.copy(alpha = 0.85f), topLeft = Offset(panelX - 4f, panelY - 2f), size = Size(layoutResult.size.width + 8f, layoutResult.size.height + 4f))
    drawText(layoutResult, topLeft = Offset(panelX, panelY))
}

/**
 * Ctrl+hover popup: rectangle with compact bid/ask volume table for the candle under cursor.
 * When Ctrl pressed and mouse over footprint candle — shows price levels table.
 */
fun DrawScope.drawFootprintPopup(
    mousePosition: Offset,
    candles: List<FootprintCandle>,
    priceRange: PriceRange,
    config: ChartConfig,
    chartLayout: ChartLayout,
    textMeasurer: TextMeasurer,
    scrollOffset: Float = 0f,
    zoomLevel: Float = 1f,
) {
    val candleMetrics = calculateCandleMetrics(zoomLevel)
    val totalW = candleMetrics.width + candleMetrics.spacing
    val virtualX = mousePosition.x - chartLayout.chartMainArea.left + scrollOffset
    val candleIndex = (virtualX / totalW).toInt().coerceIn(0, candles.size - 1)
    val candle = candles.getOrNull(candleIndex) ?: return
    if (candle.levels.isEmpty()) return

    // Find price level nearest to mouse Y
    val mousePrice = priceFromY(mousePosition.y - chartLayout.chartMainArea.top, priceRange, chartLayout.chartMainArea.height)
    var centerIdx = candle.levels.indexOfFirst { it.priceFloat >= mousePrice }
    if (centerIdx < 0) centerIdx = candle.levels.size / 2

    val visibleLevels = 11 // show ~11 levels around cursor
    val halfVisible = visibleLevels / 2
    val startIdx = (centerIdx - halfVisible).coerceAtLeast(0)
    val endIdx = (centerIdx + halfVisible).coerceAtMost(candle.levels.size)
    val viewLevels = candle.levels.subList(startIdx, endIdx)

    val title = "   Price        Bid    Ask"
    val titleStyle = TextStyle(color = Color(0xFF5B9BD5), fontSize = 8.sp, fontFamily = FontFamily.Monospace)
    val titleLayout = textMeasurer.measure(AnnotatedString(title), titleStyle)

    val lines = viewLevels.map { level ->
        val priceStr = level.priceFloat.toLong().toString().padStart(8)
        val bidStr = level.bidVolumeFloat.toLong().toString().padStart(7)
        val askStr = level.askVolumeFloat.toLong().toString().padStart(7)
        "  $priceStr  $bidStr  $askStr"
    }

    val rowStyle = TextStyle(color = config.axisTextColor, fontSize = 8.sp, fontFamily = FontFamily.Monospace)
    val rowLayouts = lines.map { textMeasurer.measure(AnnotatedString(it), rowStyle) }
    val maxRowW = rowLayouts.maxOfOrNull { it.size.width } ?: titleLayout.size.width

    val panelW = maxRowW + 10f
    val panelH = titleLayout.size.height + rowLayouts.sumOf { it.size.height } + 8f
    val gapBetweenRows = 1.5f

    // Position: right of candle if space, else left
    val candleX = candleIndex * totalW - scrollOffset + candleMetrics.width / 2
    val panelX = if (candleX + panelW + 20f > chartLayout.chartMainArea.width) maxOf(candleX - panelW - 10f, 0f) else candleX + 10f
    val panelY = maxOf(mousePosition.y - chartLayout.chartMainArea.top - panelH / 2, 0f)

    // Background
    drawRect(color = config.backgroundColor.copy(alpha = 0.92f), topLeft = Offset(panelX, panelY), size = Size(panelW, panelH))
    drawRect(color = Color(0xFF5B9BD5).copy(alpha = 0.3f), topLeft = Offset(panelX, panelY), size = Size(panelW, panelH), style = androidx.compose.ui.graphics.drawscope.Stroke(1f))

    // Title
    drawText(titleLayout, topLeft = Offset(panelX + 4f, panelY + 3f))

    // Rows with bid/ask coloring
    var yOff = panelY + titleLayout.size.height + 4f
    viewLevels.forEachIndexed { i, level ->
        val row = rowLayouts[i]
        // Highlight current level
        val isCurrent = (startIdx + i == centerIdx)
        if (isCurrent) {
            drawRect(color = Color.White.copy(alpha = 0.08f), topLeft = Offset(panelX + 2f, yOff - 1f), size = Size(panelW - 4f, row.size.height + 2f))
        }
        drawText(row, topLeft = Offset(panelX + 4f, yOff))
        yOff += row.size.height + gapBetweenRows
    }
}
