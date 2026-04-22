package com.aandios.nous.feature.dom.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
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
    val domOptions by domViewModel.domOptions.collectAsState()
    val orderQuantity by domViewModel.orderQuantity.collectAsState()
    val isTradingEnabled by domViewModel.isTradingEnabled.collectAsState()
    val symbolTickSize by domViewModel.symbolTickSize.collectAsState()
    val selectedPrice by domViewModel.selectedPrice.collectAsState()
    
    // Инкрементальные данные
    val incrementalBids by domViewModel.incrementalBids.collectAsState()
    val incrementalAsks by domViewModel.incrementalAsks.collectAsState()
    val incrementalBestBid by domViewModel.incrementalBestBid.collectAsState()
    val incrementalBestAsk by domViewModel.incrementalBestAsk.collectAsState()
    val incrementalBestBidQuantity by domViewModel.incrementalBestBidQuantity.collectAsState()
    val incrementalBestAskQuantity by domViewModel.incrementalBestAskQuantity.collectAsState()
    
    // Вычисляем отображаемый unified order book с агрегацией из инкрементальных данных
    val displayUnifiedOrderBook =
        remember(domOptions.aggregation, symbolTickSize, incrementalBids, incrementalAsks, incrementalBestBid, incrementalBestAsk) {
            // Создаём UnifiedOrderBook из инкрементальных данных
            val priceMap = mutableMapOf<String, OrderBookLevel>()
            
            val bestBid = incrementalBestBid
            val bestAsk = incrementalBestAsk
            
            // Добавляем bids, фильтруя по bestBid:
            // Bid не может быть выше bestBid (лучший bid — самая высокая цена покупки).
            // Если цена > bestBid — это stale данные, пропускаем.
            incrementalBids.forEach { (price, quantity) ->
                if (bestBid != null && price > bestBid) {
                    return@forEach
                }
                priceMap[price.toString()] = OrderBookLevel(
                    price = price.toString(),
                    quantity = quantity.toString(),
                    bidQty = quantity.toString(),
                    askQty = ""
                )
            }
            
            // Добавляем asks, фильтруя по bestAsk:
            // Ask не может быть ниже bestAsk (лучший ask — самая низкая цена продажи).
            // Если цена < bestAsk — это stale данные, пропускаем.
            incrementalAsks.forEach { (price, quantity) ->
                if (bestAsk != null && price < bestAsk) {
                    return@forEach
                }
                val existing = priceMap[price.toString()]
                if (existing != null) {
                    priceMap[price.toString()] = existing.copy(
                        askQty = quantity.toString()
                    )
                } else {
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
        }
    
    // Данные для панели ордеров из инкрементальных данных
    val panelBestBid = incrementalBestBid
    val panelBestAsk = incrementalBestAsk
    val panelOrderBook = OrderBook(
        symbol = domOptions.symbol.symbol,
        bids = incrementalBids
            .filter { (price, _) -> panelBestBid == null || price <= panelBestBid }
            .map { (price, quantity) ->
                OrderBookLevel(price = price.toString(), quantity = quantity.toString())
            },
        asks = incrementalAsks
            .filter { (price, _) -> panelBestAsk == null || price >= panelBestAsk }
            .map { (price, quantity) ->
                OrderBookLevel(price = price.toString(), quantity = quantity.toString())
            },
        timestamp = System.currentTimeMillis()
    )
    
    // BookTicker из инкрементальных данных
    val displayBookTicker = BookTicker(
        symbol = domOptions.symbol.symbol,
        bestBid = incrementalBestBid ?: 0.0,
        bestBidQty = incrementalBestBidQuantity ?: 0.0,
        bestAsk = incrementalBestAsk ?: 0.0,
        bestAskQty = incrementalBestAskQuantity ?: 0.0,
        lastPrice = 0.0,
        timestamp = System.currentTimeMillis()
    )

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
            Column(modifier = Modifier.fillMaxSize()) {
                Box(modifier = Modifier.weight(1f)) {
                    when (domOptions.mode) {
                         DomMode.SPLIT -> {
                             SplitDomContent(
                                  orderBook = panelOrderBook,
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
                                unifiedOrderBook = displayUnifiedOrderBook,
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
        // Панель размещения ордеров (фиксированная высота)
        val symbol = domOptions.symbol.symbol
        val bestBidPrice = displayBookTicker.bestBid
        val bestAskPrice = displayBookTicker.bestAsk
        
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
