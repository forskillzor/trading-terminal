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
import com.aandios.nous.feature.dom.domain.DomMode
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
    val orderQuantity by domViewModel.orderQuantity.collectAsState()
    val isTradingEnabled by domViewModel.isTradingEnabled.collectAsState()
    val aggregationLevel by domViewModel.aggregationLevel.collectAsState()
    val subscriptionDepth by domViewModel.subscriptionDepth.collectAsState()
    val tradingProvider by domViewModel.tradingProvider.collectAsState()
    val tradingSymbol by domViewModel.tradingSymbol.collectAsState()
    val depthLimit by domViewModel.depthLimit.collectAsState()
    val aggregationTime by domViewModel.aggregationTime.collectAsState()
    var domMode by remember { mutableStateOf(DomMode.NINJA) }

    // Подписка на данные при первом запуске
    LaunchedEffect(Unit) {
        // Используем новый унифицированный поток вместо отдельных подписок
        domViewModel.subscribeToUnifiedOrderBook("BTCUSDT")
        // Оставляем старые подписки для обратной совместимости (можно удалить позже)
        domViewModel.subscribeToOrderBook("BTCUSDT")
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
        modifier = Modifier.width(350.dp).fillMaxHeight(),
        unifiedOrderBook = unifiedOrderBook,
        domMode = domMode,
        onDomModeChanged = { newMode -> domMode = newMode },
        aggregationLevel = aggregationLevel,
        onAggregationLevelChanged = { level -> domViewModel.updateAggregationLevel(level) },
        subscriptionDepth = subscriptionDepth,
        onSubscriptionDepthChanged = { depth -> domViewModel.updateSubscriptionDepth(depth) },
        tradingProvider = tradingProvider,
        onTradingProviderChanged = { provider -> domViewModel.updateTradingProvider(provider) },
        tradingSymbol = tradingSymbol,
        onTradingSymbolChanged = { symbol -> domViewModel.updateTradingSymbol(symbol) },
        depthLimit = depthLimit,
        onDepthLimitChanged = { limit -> domViewModel.updateDepthLimit(limit) },
        aggregationTime = aggregationTime,
        onAggregationTimeChanged = { time -> domViewModel.updateAggregationTime(time) }
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