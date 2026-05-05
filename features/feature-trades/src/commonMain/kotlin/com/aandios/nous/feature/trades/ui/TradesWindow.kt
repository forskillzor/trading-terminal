package com.aandios.nous.feature.trades.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.aandios.nous.core.ui.theme.TradingTerminalTheme
import com.aandios.nous.feature.trades.di.initKoinForPreview
import org.koin.compose.KoinContext
import org.koin.compose.koinInject
import org.koin.core.context.stopKoin

/**
 * Trades-панель для использования в preview (или изолированного теста).
 * Инжектит TradesViewModel через Koin и делегирует полной сигнатуре.
 */
@Composable
fun TradesWindow() {
    val tradesViewModel: TradesViewModel = koinInject()
    TradesWindow(tradesViewModel = tradesViewModel)
}

/**
 * Trades-панель для использования внутри MainScreen (и др. композитов).
 * Принимает TradesViewModel напрямую (чтобы не плодить лишних экземпляров при factory-scope).
 */
@Composable
fun TradesWindow(
    tradesViewModel: TradesViewModel,
    modifier: Modifier = Modifier,
) {
    var currentSymbol by remember { mutableStateOf("BTCUSDT") }

    LaunchedEffect(currentSymbol) {
        tradesViewModel.subscribeToTrades(currentSymbol)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        TradesWidget(
            viewModel = tradesViewModel,
            currentSymbol = currentSymbol,
            onSymbolChanged = { currentSymbol = it },
            modifier = Modifier.fillMaxSize()
        )
    }
}

fun main() = application {
    stopKoin()
    initKoinForPreview()

    Window(
        onCloseRequest = ::exitApplication,
        title = "Nous Platform • Trades Preview",
        state = rememberWindowState(width = 400.dp, height = 600.dp)
    ) {
        KoinContext {
            TradingTerminalTheme {
                TradesWindow()
            }
        }
    }
}
