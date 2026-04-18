package com.aandios.nous.feature.dom.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.aandios.nous.core.ui.theme.TradingTerminalTheme
import com.aandios.nous.feature.dom.di.initKoinForPreview
import com.aandios.nous.feature.dom.domain.*
import com.aandios.nous.feature.dom.domain.model.AggregationLevel
import com.aandios.nous.feature.dom.ui.header.DomHeader
import com.aandios.nous.feature.dom.ui.split.SplitDomContent
import com.aandios.nous.feature.dom.ui.unified.UnifiedDomContent
import org.koin.compose.KoinContext
import org.koin.compose.koinInject
import org.koin.core.context.stopKoin

@Composable
fun DomWindow() {
    val domViewModel: DomViewModel = koinInject()
    val orderBook by domViewModel.orderBook.collectAsState()
    val bookTicker by domViewModel.bookTicker.collectAsState()
    val unifiedOrderBook by domViewModel.unifiedOrderBook.collectAsState()
    val domOptions by domViewModel.domOptions.collectAsState()
    val orderQuantity by domViewModel.orderQuantity.collectAsState()
    val isTradingEnabled by domViewModel.isTradingEnabled.collectAsState()
    val symbolTickSize by domViewModel.symbolTickSize.collectAsState()
    val selectedPrice by domViewModel.selectedPrice.collectAsState()

    // Вычисляем отображаемый unified order book с агрегацией
    val displayUnifiedOrderBook =
        remember(unifiedOrderBook, orderBook, bookTicker, domOptions.aggregation, symbolTickSize) {
            val baseUnified = unifiedOrderBook ?: orderBook?.let {
                UnifiedOrderBook.fromOrderBook(it, bookTicker)
            }
            if (baseUnified != null && domOptions.aggregation != AggregationLevel.BaseTick && symbolTickSize != null) {
                baseUnified.aggregate(domOptions.aggregation, symbolTickSize!!)
            } else {
                baseUnified
            }
        }

    // Данные для панели ордеров
    val panelOrderBook = orderBook ?: displayUnifiedOrderBook?.toOrderBook()

    Column(Modifier.fillMaxSize()) {
        DomHeader(
            domOptions = domOptions,
            symbolTickSize = symbolTickSize,
            onDomOptionsChanged = { newOptions -> domViewModel.updateDomOptions(newOptions) }
        )
        // Контент DOM (занимает всё доступное пространство)
        Box(
            modifier = Modifier
                .weight(1f)
                .background(MaterialTheme.colorScheme.surface)
                .fillMaxWidth()
        ) {
            val dataAvailable = when (domOptions.mode) {
                DomMode.SPLIT -> orderBook != null
                DomMode.UNIFIED -> displayUnifiedOrderBook != null
            }
            if (!dataAvailable) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp).align(Alignment.Center),
                    strokeWidth = 2.dp
                )
            } else {
                Column(modifier = Modifier.fillMaxSize()) {
                    Box(modifier = Modifier.weight(1f)) {
                        when (domOptions.mode) {
                            DomMode.SPLIT -> {
                                SplitDomContent(
                                    orderBook = orderBook!!,
                                    bookTicker = bookTicker,
                                    selectedPrice = selectedPrice,
                                    splitViewMode = domOptions.splitViewMode,
                                    baseTickSize = symbolTickSize,
                                    onPriceSelected = { price -> domViewModel.selectPrice(price) },
                                    modifier = Modifier.fillMaxSize()
                                )
                            }

                            DomMode.UNIFIED -> {
                                UnifiedDomContent(
                                    unifiedOrderBook = displayUnifiedOrderBook!!,
                                    aggregationLevel = domOptions.aggregation,
                                    baseTickSize = symbolTickSize,
                                    selectedPrice = selectedPrice,
                                    onPriceSelected = { price -> domViewModel.selectPrice(price) },
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                    }
                }
            }
        }
        // Панель размещения ордеров (фиксированная высота)
        OrderPlacementPanel(
            orderBook = panelOrderBook,
            selectedPrice = selectedPrice,
            orderQuantity = orderQuantity,
            onQuantityChanged = { qty -> domViewModel.updateOrderQuantity(qty) },
            onTradingCommand = { command -> domViewModel.executeCommand(command) },
            onCommandResult = { /* результат уже обрабатывается во ViewModel */ },
            isTradingEnabled = isTradingEnabled,
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
        )
    }
}

fun main() = application {
    stopKoin()
    initKoinForPreview()

    Window(
        onCloseRequest = ::exitApplication,
        title = "Nous Platform • DOM Preview",
        state = rememberWindowState(width = 300.dp, height = 800.dp)
    ) {
        KoinContext {
            TradingTerminalTheme {
                DomWindow()
            }
        }
    }
}
