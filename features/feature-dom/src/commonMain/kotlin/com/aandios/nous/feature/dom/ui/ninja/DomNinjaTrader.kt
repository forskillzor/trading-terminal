package com.aandios.nous.feature.dom.ui.ninja
import com.aandios.nous.feature.dom.domain.AggregationLevel

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.border
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aandios.nous.api.market.model.BookTicker
import com.aandios.nous.api.market.model.orderbook.OrderBookLevel
import com.aandios.nous.feature.dom.domain.UnifiedOrderBook
import kotlin.math.abs
import kotlin.math.max

@Composable
fun DomNinjaTrader(
    bids: List<OrderBookLevel>,
    asks: List<OrderBookLevel>,
    bookTicker: BookTicker?,
    selectedPrice: Double?,
    onPriceSelected: (Double) -> Unit,
    modifier: Modifier = Modifier
) {
    // Объединяем bids и asks в единый список уровней с bidQty и askQty
    val mergedLevels = remember(bids, asks) {
        val priceMap = mutableMapOf<String, OrderBookLevel>()
        // Добавляем bids
        bids.forEach { level ->
            priceMap[level.price] = level.copy(
                bidQty = level.quantity,
                askQty = ""
            )
        }
        // Добавляем asks, объединяя с существующими ценами
        asks.forEach { level ->
            val existing = priceMap[level.price]
            if (existing != null) {
                // Цена есть в bids, обновляем askQty
                priceMap[level.price] = existing.copy(
                    askQty = level.quantity
                )
            } else {
                // Новая цена только в asks
                priceMap[level.price] = level.copy(
                    bidQty = "",
                    askQty = level.quantity
                )
            }
        }
        // Сортируем по цене в порядке убывания (как в стакане: сверху asks, снизу bids)
        priceMap.values.sortedByDescending { it.price.toDoubleOrNull() ?: 0.0 }
    }

    // Находим максимальный объем для масштабирования (учитываем и bidQty, и askQty)
    val maxVolume = remember(mergedLevels) {
        mergedLevels.maxOfOrNull { level ->
            max(
                level.bidQty.toDoubleOrNull() ?: 0.0,
                level.askQty.toDoubleOrNull() ?: 0.0
            )
        } ?: 1.0
    }

    // Состояние скролла для автоматического скролла до цен из bookticker
    val lazyListState = rememberLazyListState()

    // Находим индекс цены для скролла (лучшая цена bid или ask из bookticker)
    val scrollToIndex = remember(bookTicker, mergedLevels) {
        if (bookTicker == null) {
            return@remember null
        }
        
        if (mergedLevels.isEmpty()) {
            return@remember null
        }
        

        // Используем среднюю цену между bestBid и bestAsk для центрирования
        val targetPrice = (bookTicker.bestBid + bookTicker.bestAsk) / 2.0

        // Поскольку mergedLevels отсортированы по убыванию, ищем ближайшую цену
        var closestIndex = 0
        var minDiff = Double.MAX_VALUE
        
        mergedLevels.forEachIndexed { index, level ->
            val price = level.price.toDoubleOrNull() ?: return@forEachIndexed
            val diff = abs(price - targetPrice)
            
            // Если найдено точное совпадение, сразу возвращаем индекс
            if (diff < 0.000001) {
                return@remember index
            }
            
            if (diff < minDiff) {
                minDiff = diff
                closestIndex = index
            }
        }
        
        // Возвращаем индекс с небольшим смещением вверх, чтобы цена была видна в центре
        val visibleItemCount = 10
        val centeredIndex = max(0, closestIndex - visibleItemCount / 2)

        centeredIndex
    }

    // Автоматический скролл до цены из bookticker
    LaunchedEffect(scrollToIndex) {
        if (scrollToIndex != null) {

            // Плавный скролл с анимацией
            try {
                lazyListState.animateScrollToItem(
                    index = scrollToIndex,
                    scrollOffset = 0
                )
                println("[DOM DEBUG] Scroll completed successfully to index $scrollToIndex")
            } catch (e: Exception) {
                println("[DOM DEBUG] Scroll error: ${e.message}")
                // Пробуем альтернативный метод скролла
                try {
                    lazyListState.scrollToItem(scrollToIndex, 0)
                    println("[DOM DEBUG] Scroll with scrollToItem completed")
                } catch (e2: Exception) {
                    println("[DOM DEBUG] Both scroll methods failed: ${e2.message}")
                }
            }
        } else {
            println("[DOM DEBUG] No scroll index, skipping scroll")
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        // Заголовок
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Bid Vol",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.weight(0.8f)
            )
            Text(
                text = "Price",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.weight(0.6f)
            )
            Text(
                text = "Ask Vol",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.weight(0.8f)
            )
        }

        // Единый LazyColumn со всеми уровнями
        LazyColumn(
            state = lazyListState,
            modifier = Modifier.weight(1f)
        ) {
            items(
                items = mergedLevels,
                key = { "level-${it.price}" }
            ) { level ->
                NinjaTraderRowUnified(
                    level = level,
                    maxVolume = maxVolume,
                    selectedPrice = selectedPrice,
                    bestBid = bookTicker?.bestBid,
                    bestAsk = bookTicker?.bestAsk,
                    onPriceClick = onPriceSelected
                )
            }
        }

        // Spread (разница) - можно оставить закомментированным или доработать
        // val bestBid = bids.firstOrNull()?.price?.toDoubleOrNull()
        // val bestAsk = asks.firstOrNull()?.price?.toDoubleOrNull()
        // ...
    }
}

@Composable
fun NinjaTraderDomUnified(
    unifiedOrderBook: UnifiedOrderBook?,
    selectedPrice: Double?,
    onPriceSelected: (Double) -> Unit,
    aggregationLevel: AggregationLevel = AggregationLevel.TICK_0_1,
    modifier: Modifier = Modifier
) {
    val levels = unifiedOrderBook?.levels ?: emptyList()
    val maxVolume = unifiedOrderBook?.maxVolume() ?: 1.0
    
    // Состояние скролла для автоматического скролла до цен из bookticker
    val lazyListState = rememberLazyListState()
    
    // Находим индекс цены для скролла (лучшая цена bid или ask из unifiedOrderBook)
    val scrollToIndex = remember(unifiedOrderBook, levels) {
        if (unifiedOrderBook == null || levels.isEmpty()) {
            return@remember null
        }
        
        val bestBid = unifiedOrderBook.bestBid
        val bestAsk = unifiedOrderBook.bestAsk
        if (bestBid == null || bestAsk == null) {
            return@remember null
        }
        
        // Используем среднюю цену между bestBid и bestAsk для центрирования
        val targetPrice = (bestBid + bestAsk) / 2.0
        
        // Поскольку levels отсортированы по убыванию, ищем ближайшую цену
        var closestIndex = 0
        var minDiff = Double.MAX_VALUE
        
        levels.forEachIndexed { index, level ->
            val price = level.price.toDoubleOrNull() ?: return@forEachIndexed
            val diff = abs(price - targetPrice)
            
            // Если найдено точное совпадение, сразу возвращаем индекс
            if (diff < 0.000001) {
                return@remember index
            }
            
            if (diff < minDiff) {
                minDiff = diff
                closestIndex = index
            }
        }
        
        // Возвращаем индекс с небольшим смещением вверх, чтобы цена была видна в центре
        val visibleItemCount = 10
        val centeredIndex = max(0, closestIndex - visibleItemCount / 2)
        
        centeredIndex
    }
    
    // Автоматический скролл до цены из bookticker
    LaunchedEffect(scrollToIndex) {
        if (scrollToIndex != null) {
            // Плавный скролл с анимацией
            try {
                lazyListState.animateScrollToItem(
                    index = scrollToIndex,
                    scrollOffset = 0
                )
                println("[DOM DEBUG] Unified scroll completed successfully to index $scrollToIndex")
            } catch (e: Exception) {
                println("[DOM DEBUG] Unified scroll error: ${e.message}")
                // Пробуем альтернативный метод скролла
                try {
                    lazyListState.scrollToItem(scrollToIndex, 0)
                    println("[DOM DEBUG] Unified scroll with scrollToItem completed")
                } catch (e2: Exception) {
                    println("[DOM DEBUG] Both scroll methods failed: ${e2.message}")
                }
            }
        } else {
            println("[DOM DEBUG] No scroll index, skipping scroll")
        }
    }
    
    Column(modifier = modifier.fillMaxSize()) {
        // Заголовок
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Bid Vol",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.weight(0.8f)
            )
            Text(
                text = "Price",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.weight(0.6f)
            )
            Text(
                text = "Ask Vol",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.weight(0.8f)
            )
        }
        
        // Вычисляем агрегированные лучшие цены для подсветки
        val aggregatedBestBid = remember(key1 = unifiedOrderBook?.bestBid, key2 = aggregationLevel) {
            unifiedOrderBook?.bestBid?.let { aggregationLevel.roundDown(it) }
        }
        val aggregatedBestAsk = remember(key1 = unifiedOrderBook?.bestAsk, key2 = aggregationLevel) {
            unifiedOrderBook?.bestAsk?.let { aggregationLevel.roundDown(it) }
        }
        
        // Единый LazyColumn со всеми уровнями
        LazyColumn(
            state = lazyListState,
            modifier = Modifier.weight(1f)
        ) {
            items(
                items = levels,
                key = { "level-${it.price}" }
            ) { level ->
                NinjaTraderRowUnified(
                    level = level,
                    maxVolume = maxVolume,
                    selectedPrice = selectedPrice,
                    bestBid = aggregatedBestBid,
                    bestAsk = aggregatedBestAsk,
                    onPriceClick = onPriceSelected
                )
            }
        }
        
        // Spread (разница) - можно отображать, если есть данные
        if (unifiedOrderBook?.spread != null && unifiedOrderBook.spreadPercent != null) {
            NinjaTraderSpread(
                bestBid = unifiedOrderBook.bestBid ?: 0.0,
                bestAsk = unifiedOrderBook.bestAsk ?: 0.0,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            )
        }
    }
}

@Composable
private fun NinjaTraderRow(
    level: OrderBookLevel,
    isAsk: Boolean,
    maxVolume: Double,
    selectedPrice: Double?,
    onPriceClick: (Double) -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()

    val price = level.price.toDoubleOrNull() ?: return
    val quantity = level.quantity.toDoubleOrNull() ?: 0.0
    val isSelected = selectedPrice?.let { abs(it - price) < 0.000001 } ?: false

    // Цвета
    val backgroundColor = when {
        isSelected -> Color.Yellow.copy(alpha = 0.3f)
        isHovered -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        else -> Color.Transparent
    }

    val priceColor = Color.White // Белый цвет для текста цены

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .hoverable(interactionSource)
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) { onPriceClick(price) }
            .background(backgroundColor)
            .padding(horizontal = 8.dp, vertical = 1.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Bid Volume (слева - только для Bids)
        Box(
            modifier = Modifier
                .weight(0.8f)
                .height(20.dp)
        ) {
            if (!isAsk && quantity > 0) {
                // Горизонтальный объем для Bid
                val volumeWidth = (quantity / maxVolume).coerceIn(0.0, 1.0)
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(volumeWidth.toFloat())
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                )
            }
            if (!isAsk && quantity > 0) {
                Text(
                    text = formatVolume(quantity),
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp
                    ),
                    modifier = Modifier.align(Alignment.CenterStart)
                )
            }
        }

        // Price (центр)
        Text(
            text = formatPrice(price),
            color = priceColor,
            style = MaterialTheme.typography.bodySmall.copy(
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
            ),
            modifier = Modifier.weight(0.6f)
        )

        // Ask Volume (справа - только для Asks)
        Box(
            modifier = Modifier
                .weight(0.8f)
                .height(20.dp)
        ) {
            if (isAsk && quantity > 0) {
                // Горизонтальный объем для Ask
                val volumeWidth = (quantity / maxVolume).coerceIn(0.0, 1.0)
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(volumeWidth.toFloat())
                        .align(Alignment.CenterEnd)
                        .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f))
                )
            }
            if (isAsk && quantity > 0) {
                Text(
                    text = formatVolume(quantity),
                    color = MaterialTheme.colorScheme.secondary,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp
                    ),
                    modifier = Modifier.align(Alignment.CenterEnd)
                )
            }
        }
    }
}

@Composable
private fun NinjaTraderRowUnified(
    level: OrderBookLevel,
    maxVolume: Double,
    selectedPrice: Double?,
    bestBid: Double?,
    bestAsk: Double?,
    onPriceClick: (Double) -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()

    val price = level.price.toDoubleOrNull() ?: return
    val bidQty = level.bidQty.toDoubleOrNull() ?: 0.0
    val askQty = level.askQty.toDoubleOrNull() ?: 0.0
    val isSelected = selectedPrice?.let { abs(it - price) < 0.000001 } ?: false
    val isBestBid = bestBid?.let { abs(it - price) < 0.000001 } ?: false
    val isBestAsk = bestAsk?.let { abs(it - price) < 0.000001 } ?: false
    val isBestPrice = isBestBid || isBestAsk

    // Цвета
    val backgroundColor = when {
        isSelected -> Color.Yellow.copy(alpha = 0.3f)
        isHovered -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        else -> Color.Transparent
    }
    // Подсветка лучших цен: тонкая граница
    val borderColor = when {
        isBestBid -> MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
        isBestAsk -> MaterialTheme.colorScheme.secondary.copy(alpha = 0.7f)
        else -> Color.Transparent
    }
    val borderWidth = if (isBestPrice) 1.dp else 0.dp

    val priceColor = Color.White // Белый цвет для текста цены

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .hoverable(interactionSource)
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) { onPriceClick(price) }
            .background(backgroundColor)
            .border(borderWidth, borderColor, shape = androidx.compose.foundation.shape.RoundedCornerShape(2.dp))
            .padding(horizontal = 8.dp, vertical = 1.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Bid Volume (слева)
        Box(
            modifier = Modifier
                .weight(0.8f)
                .height(20.dp)
        ) {
            if (bidQty > 0) {
                // Горизонтальный объем для Bid
                val volumeWidth = (bidQty / maxVolume).coerceIn(0.0, 1.0)
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(volumeWidth.toFloat())
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                )
            }
            if (bidQty > 0) {
                Text(
                    text = formatVolume(bidQty),
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp
                    ),
                    modifier = Modifier.align(Alignment.CenterStart)
                )
            }
        }

        // Price (центр)
        Text(
            text = formatPrice(price),
            color = priceColor,
            style = MaterialTheme.typography.bodySmall.copy(
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
            ),
            modifier = Modifier.weight(0.6f)
        )

        // Ask Volume (справа)
        Box(
            modifier = Modifier
                .weight(0.8f)
                .height(20.dp)
        ) {
            if (askQty > 0) {
                // Горизонтальный объем для Ask
                val volumeWidth = (askQty / maxVolume).coerceIn(0.0, 1.0)
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(volumeWidth.toFloat())
                        .align(Alignment.CenterEnd)
                        .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f))
                )
            }
            if (askQty > 0) {
                Text(
                    text = formatVolume(askQty),
                    color = MaterialTheme.colorScheme.secondary,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp
                    ),
                    modifier = Modifier.align(Alignment.CenterEnd)
                )
            }
        }
    }
}

@Composable
private fun NinjaTraderSpread(
    bestBid: Double,
    bestAsk: Double,
    modifier: Modifier = Modifier
) {
    val spread = bestAsk - bestBid
    val spreadPercent = (spread / bestBid) * 100

    Row(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = formatPrice(bestBid),
            color = MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.bodySmall.copy(
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "SPREAD",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelSmall
            )
            Text(
                text = "${formatPrice(spread)} (${"%.2f".format(spreadPercent)}%)",
                color = Color.Yellow,
                style = MaterialTheme.typography.labelSmall
            )
        }

        Text(
            text = formatPrice(bestAsk),
            color = MaterialTheme.colorScheme.secondary,
            style = MaterialTheme.typography.bodySmall.copy(
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
        )
    }
}

private fun formatPrice(price: Double): String {
    return when {
        price >= 1000 -> String.format("%.2f", price)
        price >= 100 -> String.format("%.3f", price)
        price >= 10 -> String.format("%.4f", price)
        price >= 1 -> String.format("%.5f", price)
        else -> String.format("%.6f", price)
    }
}

private fun formatVolume(volume: Double): String {
    return when {
        volume >= 1000 -> String.format("%.1fk", volume / 1000)
        volume >= 100 -> String.format("%.0f", volume)
        volume >= 10 -> String.format("%.1f", volume)
        else -> String.format("%.2f", volume)
    }
}