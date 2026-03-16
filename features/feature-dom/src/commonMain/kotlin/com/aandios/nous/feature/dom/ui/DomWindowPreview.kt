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
    var _selectedPrice by remember {mutableStateOf<Double?>(null)}
    val orderBook by domViewModel.orderBook.collectAsState()
    val bestPrices by domViewModel.bestPrices.collectAsState()
    DomWidget(
        orderBook = orderBook,
        bookTicker = bestPrices,
        selectedPrice = _selectedPrice,
        onPriceSelected = {
            _selectedPrice = it
        },
        orderQuantity = "10",
        onQuantityChanged = { quantity -> domViewModel.updateOrderQuantity(quantity) },
        onCreateBuyMarket = { domViewModel.createBuyMarketCommand() },
        onCreateSellMarket = { domViewModel.createSellMarketCommand() },
        onCreateBuyLimit = {domViewModel.createBuyLimitCommand()},
        onCreateSellLimit = {domViewModel.createSellLimitCommand()},
        onCreateBuyBestBid = {domViewModel.createBuyBestBidCommand()},
        onCreateSellBestAsk = {domViewModel.createSellBestAskCommand()},
        onCreateTradeOff = { domViewModel.createTradeOffCommand() },
        onExecuteCommand = { command -> domViewModel.executeCommand(command) },
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
