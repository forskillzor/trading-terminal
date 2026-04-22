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
import com.aandios.nous.api.market.model.BookTicker
import com.aandios.nous.api.market.model.orderbook.OrderBook
import com.aandios.nous.api.market.model.orderbook.OrderBookLevel
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
    
    // Инкрементальные данные (используются при глубине > 100)
    val incrementalBids by domViewModel.incrementalBids.collectAsState()
    val incrementalAsks by domViewModel.incrementalAsks.collectAsState()
    val incrementalBestBid by domViewModel.incrementalBestBid.collectAsState()
    val incrementalBestAsk by domViewModel.incrementalBestAsk.collectAsState()
    val incrementalBestBidQuantity by domViewModel.incrementalBestBidQuantity.collectAsState()
    val incrementalBestAskQuantity by domViewModel.incrementalBestAskQuantity.collectAsState()
    
    // Определяем, используем ли инкрементальный режим (глубина > 100)
    val useIncremental = domOptions.depth.value > 100
    
    // Вычисляем отображаемый unified order book с агрегацией
    val displayUnifiedOrderBook =
        remember(unifiedOrderBook, orderBook, bookTicker, domOptions.aggregation, symbolTickSize, incrementalBids, incrementalAsks, useIncremental) {
            if (useIncremental) {
                // Создаём UnifiedOrderBook из инкрементальных данных
                val priceMap = mutableMapOf<String, OrderBookLevel>()
                
                // Добавляем bids
                incrementalBids.forEach { (price, quantity) ->
                    priceMap[price.toString()] = OrderBookLevel(
                        price = price.toString(),
                        quantity = quantity.toString(),
                        bidQty = quantity.toString(),
                        askQty = ""
                    )
                }
                
                // Добавляем asks, объединяя с существующими ценами
                incrementalAsks.forEach { (price, quantity) ->
                    val existing = priceMap[price.toString()]
                    if (existing != null) {
                        // Цена есть в bids, обновляем askQty
                        priceMap[price.toString()] = existing.copy(
                            askQty = quantity.toString()
                        )
                    } else {
                        // Новая цена только в asks
                        priceMap[price.toString()] = OrderBookLevel(
                            price = price.toString(),
                            quantity = quantity.toString(),
                            bidQty = "",
                            askQty = quantity.toString()
                        )
                    }
                }
                
                // Сортируем по цене в порядке убывания (как в стакане)
                val sortedLevels = priceMap.values.sortedByDescending { 
                    it.price.toDoubleOrNull() ?: 0.0 
                }
                
                val bestBid = incrementalBestBid ?: bookTicker?.bestBid
                val bestAsk = incrementalBestAsk ?: bookTicker?.bestAsk
                val spread = if (bestBid != null && bestAsk != null) bestAsk - bestBid else null
                val spreadPercent = if (bestBid != null && spread != null) (spread / bestBid) * 100 else null
                
                val unified = UnifiedOrderBook(
                    symbol = domOptions.symbol.symbol,
                    levels = sortedLevels,
                    timestamp = System.currentTimeMillis(),
                    bestBid = bestBid,
                    bestAsk = bestAsk,
                    spread = spread,
                    spreadPercent = spreadPercent
                )
                if (domOptions.aggregation != AggregationLevel.BaseTick && symbolTickSize != null) {
                    unified.aggregate(domOptions.aggregation, symbolTickSize!!)
                } else {
                    unified
                }
            } else {
                val baseUnified = unifiedOrderBook ?: orderBook?.let {
                    UnifiedOrderBook.fromOrderBook(it, bookTicker)
                }
                if (baseUnified != null && domOptions.aggregation != AggregationLevel.BaseTick && symbolTickSize != null) {
                    baseUnified.aggregate(domOptions.aggregation, symbolTickSize!!)
                } else {
                    baseUnified
                }
            }
        }
    
    // Данные для панели ордеров
    val panelOrderBook = if (useIncremental) {
        // Создаём OrderBook из инкрементальных данных
        val bids = incrementalBids.map { (price, quantity) ->
            OrderBookLevel(price = price.toString(), quantity = quantity.toString())
        }
        val asks = incrementalAsks.map { (price, quantity) ->
            OrderBookLevel(price = price.toString(), quantity = quantity.toString())
        }
        OrderBook(
            symbol = domOptions.symbol.symbol,
            bids = bids,
            asks = asks,
            timestamp = System.currentTimeMillis()
        )
    } else {
        orderBook ?: displayUnifiedOrderBook?.toOrderBook()
    }
    
    // BookTicker для split режима (используется в DomSpread)
    val displayBookTicker = if (useIncremental) {
        val bestBid = incrementalBestBid ?: bookTicker?.bestBid
        val bestAsk = incrementalBestAsk ?: bookTicker?.bestAsk
        val bestBidQty = incrementalBestBidQuantity ?: bookTicker?.bestBidQty ?: 0.0
        val bestAskQty = incrementalBestAskQuantity ?: bookTicker?.bestAskQty ?: 0.0
        if (bestBid != null && bestAsk != null) {
            BookTicker(
                symbol = domOptions.symbol.symbol,
                bestBid = bestBid,
                bestBidQty = bestBidQty,
                bestAsk = bestAsk,
                bestAskQty = bestAskQty,
                lastPrice = bookTicker?.lastPrice ?: 0.0,
                timestamp = bookTicker?.timestamp ?: System.currentTimeMillis()
            )
        } else {
            bookTicker
        }
    } else {
        bookTicker
    }

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
                DomMode.SPLIT -> panelOrderBook != null
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
                                     orderBook = panelOrderBook!!,
                                     bookTicker = displayBookTicker,
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
        val symbol = domOptions.symbol.symbol
        val bestBidPrice = displayBookTicker?.bestBid ?: panelOrderBook?.bids?.firstOrNull()?.price?.toDoubleOrNull()
        val bestAskPrice = displayBookTicker?.bestAsk ?: panelOrderBook?.asks?.firstOrNull()?.price?.toDoubleOrNull()
        
        OrderPlacementPanel(
            symbol = symbol,
            selectedPrice = selectedPrice,
            orderQuantity = orderQuantity,
            bestBidPrice = bestBidPrice,
            bestAskPrice = bestAskPrice,
            onQuantityChanged = { qty -> domViewModel.updateOrderQuantity(qty) },
            onOrderIntent = { intent -> domViewModel.handleOrderIntent(intent) },
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
