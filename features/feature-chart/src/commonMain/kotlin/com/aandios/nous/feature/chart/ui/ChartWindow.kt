package com.aandios.nous.feature.chart.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.aandios.nous.core.ui.theme.TradingTerminalTheme
import com.aandios.nous.feature.chart.di.initKoinForPreview
import org.koin.compose.KoinContext
import org.koin.compose.koinInject
import org.koin.core.context.stopKoin

@Composable
fun ChartWindow() {
    val chartViewModel: ChartViewModel = koinInject()
    val chartState by chartViewModel.chartState.collectAsState()
    val currentSymbol by chartViewModel.currentSymbol.collectAsState()
    val currentTimeframe by chartViewModel.currentTimeframe.collectAsState()
    val symbols by chartViewModel.symbols.collectAsState()

    var crosshairEnabled by remember { mutableStateOf(false) }

    // Загружаем данные при первом рендере
    LaunchedEffect(Unit) {
        chartViewModel.loadChart()
    }

    Box(
        modifier = Modifier
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
                    // Chart (full size)
                    CandleStickChart(
                        candles = state.candles,
                        currentPrice = state.currentPrice,
                        crosshairEnabled = crosshairEnabled,
                        onCrosshairEnabledChange = { crosshairEnabled = it },
                        modifier = Modifier.fillMaxSize()
                    )

                    // Toolbar overlay (top-left corner)
                    ChartToolbar(
                        currentSymbol = currentSymbol,
                        currentTimeframe = currentTimeframe,
                        availableSymbols = symbols,
                        onSymbolChange = { chartViewModel.selectSymbol(it) },
                        onTimeframeChange = { chartViewModel.selectTimeframe(it) },
                        crosshairEnabled = crosshairEnabled,
                        onCrosshairToggle = { crosshairEnabled = !crosshairEnabled },
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
