package com.aandios.nous.feature.dom.ui

import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.aandios.nous.core.ui.theme.TradingTerminalTheme
import com.aandios.nous.feature.dom.di.initKoinForPreview
import org.koin.compose.KoinContext
import org.koin.compose.koinInject
import org.koin.core.context.stopKoin


@Composable
fun DomPreview() {
    val domViewModel: DomViewModel = koinInject()
    var selectedPrice by remember { mutableStateOf<Double?>(null) }
    val orderBook by domViewModel.orderBook.collectAsState()
    val bookTicker by domViewModel.bookTicker.collectAsState()
    val orderQuantity by domViewModel.orderQuantity.collectAsState()
    val isTradingEnabled by domViewModel.isTradingEnabled.collectAsState()

    // Подписка на данные при первом запуске
    LaunchedEffect(Unit) {
        domViewModel.subscribeToOrderBook("BTCUSDT", depth = 20)
        domViewModel.subscribeToBookTicker("BTCUSDT")
    }

    DomWidget(
        orderBook = orderBook,
        bookTicker = bookTicker,
        selectedPrice = selectedPrice,
        onPriceSelected = { price ->
            selectedPrice = price
            domViewModel.selectPrice(price)
        },
        orderQuantity = orderQuantity,
        onQuantityChanged = { quantity -> domViewModel.updateOrderQuantity(quantity) },
        onTradingCommand = { command -> domViewModel.executeCommand(command) },
        onCommandResult = { result -> println("Trading command result: $result") },
        isTradingEnabled = isTradingEnabled,
        modifier = Modifier.width(350.dp).fillMaxHeight()
    )
}

fun main() = application {
    stopKoin()
    initKoinForPreview()

    Window(
        onCloseRequest = ::exitApplication,
        title = "Nous Platform • DOM Preview",
        state = rememberWindowState(width = 300.dp, height = 1200.dp)
    ) {
        KoinContext {
            TradingTerminalTheme(
            ) {
                DomPreview()
            }
        }
    }
}