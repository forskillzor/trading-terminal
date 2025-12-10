package com.aandios.tradingterminal

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.aandios.tradingterminal.di.initKoin
import com.aandios.tradingterminal.ui.chart.ChartScreen
import com.aandios.tradingterminal.ui.chart.ChartViewModel
import com.aandios.tradingterminal.ui.theme.TradingTerminalTheme
import org.koin.compose.koinInject

fun main() = application {
    initKoin()

    Window(
        onCloseRequest = ::exitApplication,
        title = "Crypto Terminal • v0.1",
        state = rememberWindowState(width = 1600.dp, height = 900.dp)
    ) {
        TradingTerminalTheme(
            darkTheme = true, // Всегда темная тема
            nightMode = false // Можно добавить переключатель
        ) {
            val chartViewModel: ChartViewModel = koinInject()

            ChartScreen(
                viewModel = chartViewModel,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}