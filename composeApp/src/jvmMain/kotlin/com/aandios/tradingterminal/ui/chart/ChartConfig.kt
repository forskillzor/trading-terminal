package com.aandios.tradingterminal.ui.chart

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.aandios.tradingterminal.ui.theme.ChartColors

// Конфигурация для отрисовки свечей
data class CandleStyle(
    val bullishColor: Color = ChartColors.bullish,
    val bearishColor: Color = ChartColors.bearish,
    val shadowColor: Color = ChartColors.candleShadow,
    val bodyWidth: Float = 10f,
    val shadowWidth: Float = 1f,
    val showShadows: Boolean = true,
    val showWicks: Boolean = true
)

// Конфигурация для графика
data class ChartConfig(
    val backgroundColor: Color = ChartColors.chartBackground,
    val gridColor: Color = ChartColors.gridLine,
    val axisTextColor: Color = ChartColors.axisText,
    val showGrid: Boolean = true,
    val showVolume: Boolean = true,
    val showPriceScale: Boolean = true,
    val priceScaleWidth: Dp = 60.dp,
    val candleStyle: CandleStyle = CandleStyle()
)

// Дефолтные настройки
val DefaultChartConfig = ChartConfig()