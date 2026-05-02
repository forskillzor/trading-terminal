package com.aandios.nous_platform.ui.dom

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aandios.nous.api.market.model.BookTicker
import com.aandios.nous.api.market.model.orderbook.OrderBookLevel
import com.aandios.nous.feature.dom.domain.OrderBook
import com.aandios.nous.feature.dom.domain.model.AggregationLevel
import com.aandios.nous.feature.dom.ui.DomViewModel
import com.aandios.nous.feature.dom.ui.content.DomContent
import com.aandios.nous.feature.dom.ui.footer.OrderPlacementPanel

@Composable
fun DomWidget(
    domViewModel: DomViewModel,
    modifier: Modifier = Modifier,
) {
    val domOptions by domViewModel.domOptions.collectAsState()
    val orderQuantity by domViewModel.orderQuantity.collectAsState()
    val isTradingEnabled by domViewModel.isTradingEnabled.collectAsState()
    val symbolTickSize by domViewModel.symbolTickSize.collectAsState()
    val selectedPrice by domViewModel.selectedPrice.collectAsState()

    // SnapshotStateMap — читается напрямую, Compose отслеживает entry
    val incrementalBids = domViewModel.incrementalBids
    val incrementalAsks = domViewModel.incrementalAsks

    val incrementalBestBid by domViewModel.incrementalBestBid.collectAsState()
    val incrementalBestAsk by domViewModel.incrementalBestAsk.collectAsState()
    val incrementalBestBidQuantity by domViewModel.incrementalBestBidQuantity.collectAsState()
    val incrementalBestAskQuantity by domViewModel.incrementalBestAskQuantity.collectAsState()

    // Вычисляем отображаемый unified order book с агрегацией
    val displayUnifiedOrderBook by remember(domOptions.aggregation, symbolTickSize) {
        derivedStateOf {
            buildDisplayOrderBook(
                bids = incrementalBids,
                asks = incrementalAsks,
                bestBid = incrementalBestBid,
                bestAsk = incrementalBestAsk,
                symbol = domOptions.symbol.symbol,
                aggregation = domOptions.aggregation,
                symbolTickSize = symbolTickSize
            )
        }
    }

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

    Column(
        modifier = modifier
            .fillMaxHeight()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        if (displayUnifiedOrderBook.levels.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp
                    )
                    Text(
                        text = "Подключение...",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        } else {
            // Контент DOM (занимает всё доступное пространство)
            Box(modifier = Modifier.weight(1f)) {
                DomContent(
                    orderBook = displayUnifiedOrderBook,
                    aggregationLevel = domOptions.aggregation,
                    baseTickSize = symbolTickSize,
                    selectedPrice = selectedPrice,
                    onPriceSelected = { price -> domViewModel.selectPrice(price) },
                    modifier = Modifier.fillMaxSize()
                )
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
}

/**
 * Строит OrderBook из SnapshotStateMap и BookTicker данных.
 * Копия из DomWindow.kt для embed-режима внутри composeApp.
 */
private fun buildDisplayOrderBook(
    bids: Map<Double, Double>,
    asks: Map<Double, Double>,
    bestBid: Double?,
    bestAsk: Double?,
    symbol: String,
    aggregation: AggregationLevel,
    symbolTickSize: Double?
): OrderBook {
    val priceMap = mutableMapOf<String, OrderBookLevel>()

    // Добавляем bids, фильтруя по bestBid
    bids.forEach { (price, quantity) ->
        if (bestBid != null && price > bestBid) return@forEach
        priceMap[price.toString()] = OrderBookLevel(
            price = price.toString(),
            quantity = quantity.toString(),
            bidQty = quantity.toString(),
            askQty = ""
        )
    }

    // Добавляем asks, фильтруя по bestAsk
    asks.forEach { (price, quantity) ->
        if (bestAsk != null && price < bestAsk) return@forEach
        val existing = priceMap[price.toString()]
        if (existing != null) {
            priceMap[price.toString()] = existing.copy(askQty = quantity.toString())
        } else {
            priceMap[price.toString()] = OrderBookLevel(
                price = price.toString(),
                quantity = quantity.toString(),
                bidQty = "",
                askQty = quantity.toString()
            )
        }
    }

    // Сортируем по цене в порядке убывания
    val sortedLevels = priceMap.values.sortedByDescending {
        it.price.toDoubleOrNull() ?: 0.0
    }

    val spread = if (bestBid != null && bestAsk != null) bestAsk - bestBid else null
    val spreadPercent = if (bestBid != null && spread != null) (spread / bestBid) * 100 else null

    val unified = OrderBook(
        symbol = symbol,
        levels = sortedLevels,
        timestamp = System.currentTimeMillis(),
        bestBid = bestBid,
        bestAsk = bestAsk,
        spread = spread,
        spreadPercent = spreadPercent
    )

    return if (aggregation != AggregationLevel.BaseTick && symbolTickSize != null) {
        unified.aggregate(aggregation, symbolTickSize)
    } else {
        unified
    }
}
