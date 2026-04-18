package com.aandios.nous.feature.dom.ui.split

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aandios.nous.api.market.model.BookTicker
import com.aandios.nous.api.market.model.orderbook.OrderBook
import com.aandios.nous.feature.dom.domain.SplitViewMode
import com.aandios.nous.feature.dom.ui.DomSpread
import kotlinx.coroutines.delay
import kotlin.math.max

// todo не работает агрегация. Ее здесь даже нет.
// todo сортирровка цен должна быть от большей к меньшей

@Composable
fun SplitDomContent(
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
    
    // Состояния скролла для каждой секции
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
                
                // Скроллим bid к лучшей цене (первый элемент для reversed списка)
                if (orderBook.bids.isNotEmpty()) {
                    bidListState.animateScrollToItem(0)
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
        when (splitViewMode) {
            SplitViewMode.BID_ASK -> {
                // Заголовок для ask секции
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Ask Price",
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
                
                // Ask секция (скроллится при переполнении)
                SplitDomSection(
                    levels = orderBook.asks,
                    maxVolume = maxVolume,
                    isAsk = true,
                    selectedPrice = selectedPrice,
                    baseTickSize = baseTickSize,
                    onPriceClick = { price -> onPriceSelected(price) },
                    listState = askListState,
                    modifier = Modifier.weight(1f)
                )
                
                // Spread (фиксированная высота)
                DomSpread(
                    bookTicker = bookTicker,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                )
                
                // Заголовок для bid секции
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Bid Price",
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
                
                // Bid секция (скроллится при переполнении, reversed чтобы лучшая цена была сверху)
                SplitDomSection(
                    levels = orderBook.bids.reversed(),
                    maxVolume = maxVolume,
                    isAsk = false,
                    selectedPrice = selectedPrice,
                    baseTickSize = baseTickSize,
                    onPriceClick = { price -> onPriceSelected(price) },
                    listState = bidListState,
                    modifier = Modifier.weight(1f)
                )
            }
            
            SplitViewMode.BID_ONLY -> {
                // Заголовок для bid секции
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Bid Price",
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
                
                // Только bid секция
                SplitDomSection(
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
                // Заголовок для ask секции
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Ask Price",
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
                
                // Только ask секция
                SplitDomSection(
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