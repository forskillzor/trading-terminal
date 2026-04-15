package com.aandios.nous.feature.dom.ui.split

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aandios.nous.api.market.model.BookTicker
import com.aandios.nous.api.market.model.orderbook.OrderBook
import com.aandios.nous.feature.dom.domain.SplitViewMode
import com.aandios.nous.feature.dom.ui.DomSpread
import kotlin.math.max
import kotlinx.coroutines.delay

@Composable
fun EnhancedSplitDomContent(
    orderBook: OrderBook,
    bookTicker: BookTicker?,
    selectedPrice: Double?,
    splitViewMode: SplitViewMode = SplitViewMode.BID_ASK,
    baseTickSize: Double? = null,
    onPriceSelected: (Double?) -> Unit,
    modifier: Modifier = Modifier
) {
    val maxVolume = remember(orderBook.bids, orderBook.asks) {
        max(
            orderBook.bids.maxOfOrNull { it.quantity.toDouble() } ?: 0.0,
            orderBook.asks.maxOfOrNull { it.quantity.toDouble() } ?: 0.0
        )
    }
    
    // Состояния скролла для каждого столбца
    val bidListState = rememberLazyListState()
    val askListState = rememberLazyListState()
    
    // Автоскролл через 5000мс если пользователь не скроллил
    var lastUserScrollTime by remember { mutableStateOf(System.currentTimeMillis()) }
    var isAutoScrolling by remember { mutableStateOf(false) }
    
    LaunchedEffect(bidListState.isScrollInProgress, askListState.isScrollInProgress) {
        if (bidListState.isScrollInProgress || askListState.isScrollInProgress) {
            lastUserScrollTime = System.currentTimeMillis()
            isAutoScrolling = false
        }
    }
    
    // Автоскролл к лучшим ценам через 5000мс бездействия
    LaunchedEffect(lastUserScrollTime) {
        while (true) {
            delay(100) // Проверяем каждые 100мс
            val timeSinceLastScroll = System.currentTimeMillis() - lastUserScrollTime
            if (timeSinceLastScroll > 5000 && !isAutoScrolling) {
                isAutoScrolling = true
                
                // Скроллим bid к лучшей цене (последний элемент для reversed списка)
                if (orderBook.bids.isNotEmpty()) {
                    bidListState.animateScrollToItem(orderBook.bids.size - 1)
                }
                
                // Скроллим ask к лучшей цене (первый элемент)
                askListState.animateScrollToItem(0)
                
                delay(1000) // Ждем завершения анимации
                isAutoScrolling = false
            }
        }
    }
    
    Column(
        modifier = modifier.fillMaxSize()
    ) {
        // Заголовки в зависимости от режима
        when (splitViewMode) {
            SplitViewMode.BID_ASK -> {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Bid Price",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "Size",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "Total",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Ask Price",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "Size",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "Total",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.weight(1f)
                    )
                }
                
                // Два столбца + spread
                Row(modifier = Modifier.fillMaxSize()) {
                    // Bid column (reversed чтобы лучшая цена была внизу)
                    EnhancedSplitDomSection(
                        levels = orderBook.bids.reversed(),
                        maxVolume = maxVolume,
                        isAsk = false,
                        selectedPrice = selectedPrice,
                        baseTickSize = baseTickSize,
                        onPriceClick = { price -> onPriceSelected(price) },
                        listState = bidListState,
                        modifier = Modifier
                            .weight(0.5f)
                            .fillMaxHeight()
                    )
                    
                    // Spread
                    DomSpread(
                        bookTicker = bookTicker,
                        modifier = Modifier
                            .width(120.dp)
                            .fillMaxHeight()
                    )
                    
                    // Ask column
                    EnhancedSplitDomSection(
                        levels = orderBook.asks,
                        maxVolume = maxVolume,
                        isAsk = true,
                        selectedPrice = selectedPrice,
                        baseTickSize = baseTickSize,
                        onPriceClick = { price -> onPriceSelected(price) },
                        listState = askListState,
                        modifier = Modifier
                            .weight(0.5f)
                            .fillMaxHeight()
                    )
                }
            }
            
            SplitViewMode.BID_ONLY -> {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Price",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.labelSmall
                    )
                    Text(
                        text = "Size",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.labelSmall
                    )
                    Text(
                        text = "Total",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
                
                EnhancedSplitDomSection(
                    levels = orderBook.bids.reversed(),
                    maxVolume = maxVolume,
                    isAsk = false,
                    selectedPrice = selectedPrice,
                    baseTickSize = baseTickSize,
                    onPriceClick = { price -> onPriceSelected(price) },
                    listState = bidListState,
                    modifier = Modifier.fillMaxSize()
                )
            }
            
            SplitViewMode.ASK_ONLY -> {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Price",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.labelSmall
                    )
                    Text(
                        text = "Size",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.labelSmall
                    )
                    Text(
                        text = "Total",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
                
                EnhancedSplitDomSection(
                    levels = orderBook.asks,
                    maxVolume = maxVolume,
                    isAsk = true,
                    selectedPrice = selectedPrice,
                    baseTickSize = baseTickSize,
                    onPriceClick = { price -> onPriceSelected(price) },
                    listState = askListState,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

@Composable
fun EnhancedSplitDomSection(
    levels: List<com.aandios.nous.api.market.model.orderbook.OrderBookLevel>,
    maxVolume: Double,
    isAsk: Boolean,
    selectedPrice: Double?,
    baseTickSize: Double? = null,
    onPriceClick: (Double) -> Unit,
    listState: LazyListState,
    modifier: Modifier = Modifier
) {
    SplitDomSection(
        levels = levels,
        maxVolume = maxVolume,
        isAsk = isAsk,
        selectedPrice = selectedPrice,
        baseTickSize = baseTickSize,
        onPriceClick = onPriceClick,
        modifier = modifier
    )
}