package com.aandios.nous.feature.chart.rendering

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.withTransform
import com.aandios.nous.api.market.model.FootprintCandle
import com.aandios.nous.feature.chart.model.CandleMetrics
import com.aandios.nous.feature.chart.model.PriceRange
import com.aandios.nous.feature.chart.ui.ChartConfig
import com.aandios.nous.feature.chart.utils.calculateCandleMetrics
import com.aandios.nous.feature.chart.utils.priceToY
import kotlin.math.abs

fun DrawScope.drawFootprintChart(
    candles: List<FootprintCandle>,
    priceRange: PriceRange,
    config: ChartConfig,
    chartArea: Rect,
    textMeasurer: androidx.compose.ui.text.TextMeasurer,
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
        val totalW = candleMetrics.width + candleMetrics.spacing

        for (i in visibleStartIndex until visibleEndIndex) {
            if (i in candles.indices) {
                val x = i * totalW - scrollOffset + candleMetrics.width / 2
                drawFootprintCandle(
                    candle = candles[i],
                    centerX = x,
                    priceRange = priceRange,
                    metrics = candleMetrics,
                    config = config,
                    chartHeight = chartArea.height
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
    chartHeight: Float
) {
    if (candle.levels.isEmpty()) return

    val fpConfig = config.footprintConfig
    val maxVol = candle.maxVolume.coerceAtLeast(0.00001f)
    val halfWidth = metrics.width / 2
    val maxBarWidth = halfWidth * 0.85f

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

        val bidWidth = (level.bidVolumeFloat / maxVol * maxBarWidth).coerceAtLeast(if (level.bidVolumeFloat > 0f) 1f else 0f)
        val askWidth = (level.askVolumeFloat / maxVol * maxBarWidth).coerceAtLeast(if (level.askVolumeFloat > 0f) 1f else 0f)

        // Ask volume (left side — sells, red)
        if (askWidth > 0f) {
            drawRect(
                color = fpConfig.askColor,
                topLeft = Offset(centerX - halfWidth + (maxBarWidth - askWidth), topY),
                size = Size(askWidth, levelHeight)
            )
        }

        // Bid volume (right side — buys, green)
        if (bidWidth > 0f) {
            drawRect(
                color = fpConfig.bidColor,
                topLeft = Offset(centerX + halfWidth - maxBarWidth, topY),
                size = Size(bidWidth, levelHeight)
            )
        }
    }
}
