package com.aandios.nous.feature.chart.ui.chart

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.aandios.nous.api.market.model.Candle
import com.aandios.nous.api.market.model.liquidation.LiquidationOrder
import com.aandios.nous.api.market.model.trading.TradeSide
import com.aandios.nous.feature.chart.model.PriceRange
import com.aandios.nous.feature.chart.utils.calculateCandleMetrics
import com.aandios.nous.feature.chart.utils.priceToY
import kotlin.math.min

fun DrawScope.drawLiquidationMarkers(
    orders: List<LiquidationOrder>,
    priceRange: PriceRange,
    chartWidth: Float,
    chartHeight: Float,
    scrollOffset: Float,
    candles: List<Candle>,
    timeframeMs: Long,
    zoomLevel: Float
) {
    if (orders.isEmpty() || candles.isEmpty()) return

    val metrics = calculateCandleMetrics(zoomLevel)
    val totalW = metrics.width + metrics.spacing
    val minTimestamp = candles.map { it.timestamp }.minOrNull() ?: return
    val candleWidth = totalW

    val currentTime = System.currentTimeMillis()
    val maxAgeMs = 4 * 60 * 60 * 1000L

    for (order in orders) {
        val ts = order.timestamp
        if (ts < minTimestamp) continue

        val age = currentTime - ts
        if (age > maxAgeMs) continue

        val priceY = priceToY(order.price.toFloat(), priceRange, chartHeight)
        if (priceY.isNaN() || priceY < 0f || priceY > chartHeight) continue

        val slotFromStart = (ts - minTimestamp).toFloat() / timeframeMs.toFloat()
        val x = slotFromStart * candleWidth - scrollOffset + metrics.width / 2

        if (x < -10f || x > chartWidth + 10f) continue

        val alpha = (1f - min(1f, age.toFloat() / maxAgeMs.toFloat())).coerceIn(0.15f, 1f)

        val color = when (order.side) {
            TradeSide.SELL -> Color.Red.copy(alpha = alpha)
            TradeSide.BUY -> Color.Green.copy(alpha = alpha)
        }

        val size = 6f * min(1f + order.quantity.toFloat() * 0.5f, 4f)
        val path = Path()

        if (order.side == TradeSide.SELL) {
            path.moveTo(x, priceY - size)
            path.lineTo(x + size * 0.7f, priceY)
            path.lineTo(x - size * 0.7f, priceY)
            path.close()
        } else {
            path.moveTo(x, priceY + size)
            path.lineTo(x + size * 0.7f, priceY)
            path.lineTo(x - size * 0.7f, priceY)
            path.close()
        }

        drawPath(path, color)
    }
}

fun timeframeToMs(tf: String): Long = when (tf) {
    "1m" -> 60_000L; "5m" -> 300_000L; "15m" -> 900_000L
    "30m" -> 1_800_000L; "1h" -> 3_600_000L; "4h" -> 14_400_000L
    "1d" -> 86_400_000L; "1w" -> 604_800_000L
    else -> 3_600_000L
}
