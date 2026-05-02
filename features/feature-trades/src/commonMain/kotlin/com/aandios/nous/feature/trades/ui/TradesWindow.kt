package com.aandios.nous.feature.trades.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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

@Composable
fun TradesWindow() {
    val viewModel: TradesViewModel = koinInject()

    LaunchedEffect(Unit) {
        viewModel.subscribeToTrades("BTCUSDT")
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        TradesWidget(
            viewModel = viewModel,
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
