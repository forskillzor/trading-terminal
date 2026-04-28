package com.aandios.nous.feature.chart.ui.chart

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.aandios.nous.api.market.model.Candle
import com.aandios.nous.feature.chart.ui.ChartConfig
import com.aandios.nous.feature.chart.ui.DefaultChartConfig

/**
 * Тонкая обёртка над [CandleStickChartInteraction].
 *
 * Вся логика взаимодействия (зум, скролл, crosshair, расчёт layout, Canvas)
 * находится в [CandleStickChartInteraction] для соблюдения SRP.
 *
 * Этот композабл сохраняет публичный API, ожидаемый вызывающими клиентами
 * (ChartWindow, ViewModel и т.д.).
 */
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
    CandleStickChartInteraction(
        candles = candles,
        currentPrice = currentPrice,
        modifier = modifier,
        config = config,
        showPriceScale = showPriceScale,
        priceScaleWidth = priceScaleWidth,
        crosshairEnabled = crosshairEnabled,
        onCrosshairEnabledChange = onCrosshairEnabledChange,
        onNeedMoreHistory = onNeedMoreHistory,
        historyLoadCount = historyLoadCount,
        hasMoreHistory = hasMoreHistory,
    )
}
