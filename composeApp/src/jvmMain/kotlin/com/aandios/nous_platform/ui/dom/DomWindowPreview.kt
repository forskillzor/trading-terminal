package com.aandios.nous_platform.ui.dom

import androidx.compose.foundation.layout.width
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.aandios.nous_platform.di.initKoin
import com.aandios.nous_platform.ui.theme.TradingTerminalTheme
import org.koin.compose.koinInject

@Composable
private fun DomPreview(
    domViewModel: DomViewModel
) {
    var _selectedPrice by remember {mutableStateOf<Double?>(null)}
    val orderBook by domViewModel.orderBook.collectAsState()
    val bestPrices by domViewModel.bestPrices.collectAsState()
    DomWidget(
        orderBook = orderBook,
        bestPrices = bestPrices,
        selectedPrice = _selectedPrice,
        onPriceSelected = {
            _selectedPrice = it
        },
        orderQuantity = "10",
        onQuantityChanged = { quantity -> domViewModel.updateOrderQuantity(quantity) },
        onTradingCommand = { command -> domViewModel.executeCommand(command) },
        onCommandResult = domViewModel::onCommandResult,
        isTradingEnabled = domViewModel.isTradingEnabled.collectAsState().value,
        modifier = Modifier.width(300.dp)
    )
}
fun main() = application {
    initKoin()
    val domViewModel: DomViewModel = koinInject()
    domViewModel.subscribeToOrderBook("BTCUSDT")
    domViewModel.subscribeToBestPrices("BTCUSDT")

    Window(
        onCloseRequest = ::exitApplication,
        title = "Nous Platform • v 0.1",
        state = rememberWindowState(width = 300.dp, height = 1200.dp)
    ) {
        TradingTerminalTheme(
            darkTheme = true, // Всегда темная тема
            nightMode = false // Можно добавить переключатель
        ) {
            DomPreview(domViewModel)
        }
    }
}
