package com.aandios.nous.feature.dom.ui

import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.aandios.nous.feature.dom.di.featureDomModule
import org.koin.compose.koinInject
import org.koin.core.context.startKoin

private fun initFeatureKoin() {
    startKoin {
        modules(featureDomModule)
    }
}

@Composable
private fun DomPreview(
    domViewModel: DomViewModel
) {
    var _selectedPrice by remember { mutableStateOf<Double?>(null) }
    val orderBook by domViewModel.orderBook.collectAsState()
    val bestPrices by domViewModel.bestPrices.collectAsState()
    val orderQuantity by domViewModel.orderQuantity.collectAsState()
    DomWidget(
        orderBook = orderBook,
        bookTicker = bestPrices,
        selectedPrice = _selectedPrice,
        onPriceSelected = { price ->
            _selectedPrice = price
            domViewModel.selectPrice(price)
        },
        orderQuantity = orderQuantity,
        onQuantityChanged = { quantity -> domViewModel.updateOrderQuantity(quantity) },
        onTradingCommand = { command -> domViewModel.executeCommand(command) },
        onCommandResult = { result -> println("Trading command result: $result") },
        isTradingEnabled = domViewModel.isTradingEnabled.collectAsState().value,
        modifier = Modifier.width(300.dp)
    )
}

fun main() = application {
    initFeatureKoin()
    val domViewModel: DomViewModel = koinInject()
    domViewModel.subscribeToOrderBook("BTCUSDT")
    domViewModel.subscribeToBestPrices("BTCUSDT")

    Window(
        onCloseRequest = ::exitApplication,
        title = "Nous Platform • v 0.1",
        state = rememberWindowState(width = 300.dp, height = 1200.dp)
    ) {
        MaterialTheme(
            colorScheme = darkColorScheme()
        ) {
            DomPreview(domViewModel)
        }
    }
}
