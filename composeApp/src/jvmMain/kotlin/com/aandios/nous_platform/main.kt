package com.aandios.nous_platform

import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.aandios.nous_platform.di.initKoin
import com.aandios.nous_platform.ui.main.MainScreen
import com.aandios.nous_platform.ui.chart.ChartViewModel
import com.aandios.nous_platform.ui.dom.DomViewModel
import com.aandios.nous_platform.ui.terminalLayout.TerminalLayout
import com.aandios.nous_platform.ui.terminalLayout.TerminalStateViewModel
import com.aandios.nous_platform.ui.theme.TradingTerminalTheme
import com.aandios.nous_platform.ui.trades.TradesViewModel
import org.koin.compose.koinInject

fun main() = application {
    initKoin()

    Window(
        onCloseRequest = ::exitApplication,
        title = "Nous Platform • v 0.1",
        state = rememberWindowState(width = 1600.dp, height = 1100.dp)
    ) {
        TradingTerminalTheme(
            darkTheme = true, // Всегда темная тема
            nightMode = false // Можно добавить переключатель
        ) {
            val chartViewModel: ChartViewModel = koinInject()
            val domViewModel: DomViewModel = koinInject()
            val tradesViewModel: TradesViewModel = koinInject()
            val terminalStateViewModel: TerminalStateViewModel = koinInject()

            // todo here should be TerminalLayout

            TerminalLayout(

                modifier = Modifier.fillMaxHeight(),
                terminalState = terminalStateViewModel,
                onSymbolSelected = { symbol ->
                    terminalStateViewModel.changeSymbol(symbol)
                    val timeframe = terminalStateViewModel.selectedTimeFrame.value
                    // Перезагружаем данные
                    chartViewModel.loadChart(symbol, timeframe)
                    domViewModel.subscribeToOrderBook(symbol)
                    tradesViewModel.subscribeToTrades(symbol)
                },
                onTimeframeSelected = { timeframe ->
                    terminalStateViewModel.changeTimeFrame(timeframe)
                    val symbol = terminalStateViewModel.selectedSymbol.value
                    chartViewModel.loadChart(symbol, timeframe)
                },
                {
                    MainScreen(
                        chartViewModel = chartViewModel,
                        domViewModel = domViewModel,
                        tradesViewModel = tradesViewModel,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                    )
                }
            )
        }
    }
}