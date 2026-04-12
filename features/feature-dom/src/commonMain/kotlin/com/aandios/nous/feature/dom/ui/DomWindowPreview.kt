package com.aandios.nous.feature.dom.ui

import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
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
    val unifiedOrderBook by domViewModel.unifiedOrderBook.collectAsState()
    val domOptions by domViewModel.domOptions.collectAsState()
    val orderQuantity by domViewModel.orderQuantity.collectAsState()
    val isTradingEnabled by domViewModel.isTradingEnabled.collectAsState()
    val symbolTickSize by domViewModel.symbolTickSize.collectAsState()

    // ViewModel уже инициализирует подписку с дефолтными настройками (BTCUSDT, depth=100)
    // Не нужно вызывать дополнительные подписки - это вызовет рестарт и ошибки отмены

    DomWidget(
        modifier = Modifier.width(350.dp).fillMaxHeight(),
        domOptions = domOptions,
        onDomOptionsChanged = { newOptions -> domViewModel.updateDomOptions(newOptions) }
    ) {
        DomContent(
            orderBook = orderBook,
            unifiedOrderBook = unifiedOrderBook,
            bookTicker = bookTicker,
            domOptions = domOptions,
            symbolTickSize = symbolTickSize,
            selectedPrice = selectedPrice,
            onPriceSelected = { price -> domViewModel.selectPrice(price) },
            // Order placement parameters
            orderQuantity = orderQuantity,
            onQuantityChanged = { qty -> domViewModel.updateOrderQuantity(qty) },
            onTradingCommand = { command -> domViewModel.executeCommand(command) },
            onCommandResult = { /* результат уже обрабатывается во ViewModel */ },
            isTradingEnabled = isTradingEnabled,
            modifier = Modifier.fillMaxSize()
        )
    }
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
            TradingTerminalTheme {
                DomPreview()
            }
        }
    }
}
