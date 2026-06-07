package com.aandios.nous.feature.chart.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.aandios.nous.api.market.model.Candle
import com.aandios.nous.core.ui.theme.TradingTerminalTheme
import com.aandios.nous.feature.chart.di.initKoinForPreview
import com.aandios.nous.feature.chart.ui.chart.CandleStickChart
import org.koin.compose.KoinContext
import org.koin.compose.koinInject
import org.koin.core.context.stopKoin

/**
 * Полноценное окно графика для использования внутри main приложения.
 * Получает ChartViewModel из Koin автоматически.
 */
@Composable
fun ChartWindow() {
    val chartViewModel: ChartViewModel = koinInject()
    LaunchedEffect(Unit) {
        chartViewModel.loadChart()
    }
    ChartWindow(chartViewModel = chartViewModel)
}

/**
 * Окно графика для использования внутри MainScreen (и др. композитов).
 * Принимает ChartViewModel напрямую (чтобы не плодить лишних экземпляров при factory-scope).
 *
 * Загрузку графика (chartViewModel.loadChart()) ожидается, что вызывает родительский composable.
 */
@Composable
fun ChartWindow(
    chartViewModel: ChartViewModel,
    modifier: Modifier = Modifier,
) {
    val chartState by chartViewModel.chartState.collectAsState()
    val currentSymbol by chartViewModel.currentSymbol.collectAsState()
    val currentTimeframe by chartViewModel.currentTimeframe.collectAsState()
    val symbols by chartViewModel.symbols.collectAsState()
    val historyLoadCount by chartViewModel.historyLoadCount.collectAsState()
    val hasMoreHistory by chartViewModel.hasMoreHistory.collectAsState()
    val footprintCandles by chartViewModel.footprintCandles.collectAsState()
    val liveFootprintCandle by chartViewModel.liveFootprintCandle.collectAsState()
    val footprintCurrentPrice by chartViewModel.footprintCurrentPrice.collectAsState()
    val footprintLoading by chartViewModel.footprintLoading.collectAsState()
    val footprintError by chartViewModel.footprintError.collectAsState()
    val chartMode by chartViewModel.chartMode.collectAsState()
    val symbolsWithFootprint by chartViewModel.symbolsWithFootprint.collectAsState()
    val hasMoreFootprintHistory by chartViewModel.hasMoreFootprintHistory.collectAsState()
    val footprintHistoryLoadCount by chartViewModel.footprintHistoryLoadCount.collectAsState()
    val fpAggregation by chartViewModel.fpAggregation.collectAsState()
    val formatter by chartViewModel.currentSymbolFormatter.collectAsState()

    val chartConfig = remember(fpAggregation, formatter) {
        DefaultChartConfig.copy(footprintConfig = DefaultChartConfig.footprintConfig.copy(
            aggregationLevel = fpAggregation,
            tickSize = formatter.tickSize
        ))
    }

    var crosshairEnabled by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        when (val state = chartState) {
            is ChartState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Loading chart data...",
                        color = MaterialTheme.colorScheme.onBackground,
                        fontSize = 14.sp
                    )
                }
            }
            is ChartState.Error -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Error loading chart",
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 16.sp
                        )
                        Text(
                            text = state.message,
                            color = MaterialTheme.colorScheme.onBackground,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
            }
            is ChartState.Success -> {
                Box(modifier = Modifier.fillMaxSize()) {
                    when (chartMode) {
                        ChartMode.CANDLESTICK -> {
                            CandleStickChart(
                                candles = state.candles,
                                currentPrice = state.currentPrice,
                                config = chartConfig,
                                crosshairEnabled = crosshairEnabled,
                                onCrosshairEnabledChange = { crosshairEnabled = it },
                                onNeedMoreHistory = { chartViewModel.loadMoreHistory() },
                                historyLoadCount = historyLoadCount,
                                hasMoreHistory = hasMoreHistory,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        ChartMode.FOOTPRINT -> {
                            if (footprintLoading && footprintCandles.isEmpty()) {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Text("Loading footprint data...", color = MaterialTheme.colorScheme.onBackground, fontSize = 14.sp)
                                }
                            } else if (footprintError != null && footprintCandles.isEmpty()) {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("Footprint data error", color = MaterialTheme.colorScheme.error, fontSize = 14.sp)
                                        Text(footprintError!!, color = MaterialTheme.colorScheme.onBackground, fontSize = 11.sp, modifier = Modifier.padding(top = 8.dp))
                                    }
                                }
                            } else {
                                // Merge completed + live: live overrides same startTime
                                val allFp = buildList {
                                    val liveStart = liveFootprintCandle?.startTime
                                    addAll(footprintCandles.filter { it.startTime != liveStart })
                                    liveFootprintCandle?.let { add(it) }
                                }
                                val fpToCandle = remember(allFp) {
                                    allFp.map { Candle(it.open, it.high, it.close, it.low, it.startTime, it.maxVolume) }
                                }
                                CandleStickChart(
                                    candles = fpToCandle,
                                    currentPrice = footprintCurrentPrice ?: fpToCandle.lastOrNull()?.close,
                                    config = chartConfig,
                                    crosshairEnabled = crosshairEnabled,
                                    onCrosshairEnabledChange = { crosshairEnabled = it },
                                    footprintCandles = allFp,
                                    onNeedMoreHistory = { chartViewModel.loadMoreFootprintHistory() },
                                    historyLoadCount = footprintHistoryLoadCount,
                                    hasMoreHistory = hasMoreFootprintHistory,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                    }

                    ChartToolbar(
                        currentSymbol = currentSymbol,
                        currentTimeframe = currentTimeframe,
                        availableSymbols = symbols,
                        onSymbolChange = { chartViewModel.selectSymbol(it) },
                        onTimeframeChange = { chartViewModel.selectTimeframe(it) },
                        crosshairEnabled = crosshairEnabled,
                        onCrosshairToggle = { crosshairEnabled = !crosshairEnabled },
                        chartMode = chartMode,
                        onChartModeToggle = { chartViewModel.toggleChartMode() },
                        symbolsWithFootprint = symbolsWithFootprint,
                        fpAggregation = fpAggregation,
                        onFpAggregationChange = { chartViewModel.setFpAggregation(it) },
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(8.dp)
                    )
                }
            }
        }
    }
}

fun main() = application {
    stopKoin()
    initKoinForPreview()

    Window(
        onCloseRequest = ::exitApplication,
        title = "Nous Platform • Chart Preview",
        state = rememberWindowState(width = 800.dp, height = 600.dp)
    ) {
        KoinContext {
            TradingTerminalTheme {
                ChartWindow()
            }
        }
    }
}
