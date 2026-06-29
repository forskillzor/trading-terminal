package com.aandios.nous.feature.dom.ui.content

import com.aandios.nous.core.ui.format.SymbolFormatter
import com.aandios.nous.feature.dom.domain.model.AggregationLevel

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aandios.nous.feature.dom.domain.OrderBook
import kotlin.math.abs

@Composable
fun DomSection(
    orderBook: OrderBook?,
    selectedPrice: Double?,
    onPriceSelected: (Double) -> Unit,
    aggregationLevel: AggregationLevel = AggregationLevel.BaseTick,
    baseTickSize: Double? = null,
    modifier: Modifier = Modifier
) {
    val levels = orderBook?.levels ?: emptyList()
    val maxVolume = orderBook?.maxVolume() ?: 1.0

    // Состояние скролла для автоматического скролла до цен из bookticker
    val lazyListState = rememberLazyListState()

    /**
     *  Автоматический скролл до лучшей цены, если она скрыта
     *  Цель: показывать лучшую цену bid, когда она уходит за границы видимой области
     *  ПРИНЦИПЫ:
     *  1. Скроллим ТОЛЬКО если лучшая цена НЕ видна в текущем viewport
     *  2. НЕ скроллим, если пользователь активно скроллит сам
     *  Целевая цена для скролла - агрегированный лучший bid.
     *  Используем агрегацию, чтобы скроллить до того же уровня, который подсвечивается
     **/
    val scrollTargetPrice = remember(orderBook, aggregationLevel, baseTickSize) {
        val mid = orderBook?.bestBid?.let { bid ->
            orderBook.bestAsk?.let { ask -> (bid + ask) / 2.0 }
        } ?: orderBook?.bestBid
        mid?.let { price ->
            if (baseTickSize != null) aggregationLevel.roundDown(price, baseTickSize)
            else price
        }
    }

    // Автоматический скролл при изменении лучшей цены
    LaunchedEffect(scrollTargetPrice) {
        // Если нет целевой цены - ничего не делаем
        if (scrollTargetPrice == null) return@LaunchedEffect

        // Дебаунс 200 мс - ждём стабилизации цены, не дёргаемся на каждом тике

        // Если пользователь сейчас скроллит - не вмешиваемся
        if (lazyListState.isScrollInProgress) return@LaunchedEffect

        // Ищем индекс целевой цены в списке уровней
        // Linear search допустим, так как levels обычно содержит 100-200 элементов
        // и поиск выполняется редко (только при изменении лучшей цены + дебаунс)
        val targetIndex = levels.indexOfFirst { level ->
            val levelPrice = level.price.toDoubleOrNull()
            if (levelPrice == null) return@indexOfFirst false
            if (baseTickSize != null) {
                // Сравниваем через агрегационный ключ
                aggregationLevel.aggregationKey(levelPrice.toString(), baseTickSize) == 
                    aggregationLevel.aggregationKey(scrollTargetPrice.toString(), baseTickSize)
            } else {
                // fallback: сравнение с допуском
                abs(levelPrice - scrollTargetPrice) < 0.000001
            }
        }.takeIf { it >= 0 } ?: return@LaunchedEffect // цена не найдена в levels

        // Проверяем, видна ли уже целевая цена в текущем viewport
        val visibleItems = lazyListState.layoutInfo.visibleItemsInfo
        val isTargetVisible = visibleItems.any { visibleItem ->
            // Получаем уровень по индексу видимого элемента
            val visibleIndex = visibleItem.index
            if (visibleIndex in levels.indices) {
                val levelPrice = levels[visibleIndex].price.toDoubleOrNull() ?: return@any false
                if (baseTickSize != null) {
                    // Сравниваем через агрегационный ключ
                    aggregationLevel.aggregationKey(levelPrice.toString(), baseTickSize) == 
                        aggregationLevel.aggregationKey(scrollTargetPrice.toString(), baseTickSize)
                } else {
                    // fallback: сравнение с допуском
                    abs(levelPrice - scrollTargetPrice) < 0.000001
                }
            } else {
                false
            }
        }

        if (!isTargetVisible) {
            // Center the target in the viewport: scroll so target is in the middle
            val visibleCount = lazyListState.layoutInfo.visibleItemsInfo.size
            val centeredIndex = (targetIndex - visibleCount / 2).coerceAtLeast(0)
            lazyListState.animateScrollToItem(centeredIndex, 0)
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
        val aggregatedBestBid = remember(key1 = orderBook?.bestBid, key2 = aggregationLevel, key3 = baseTickSize) {
            orderBook?.bestBid?.let { bestBid ->
                if (baseTickSize != null) {
                    aggregationLevel.roundDown(bestBid, baseTickSize)
                } else {
                    bestBid // без округления, если tickSize неизвестен
                }
            }
        }
        val aggregatedBestAsk = remember(key1 = orderBook?.bestAsk, key2 = aggregationLevel, key3 = baseTickSize) {
            orderBook?.bestAsk?.let { bestAsk ->
                if (baseTickSize != null) {
                    aggregationLevel.roundDown(bestAsk, baseTickSize)
                } else {
                    bestAsk // без округления, если tickSize неизвестен
                }
            }
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
                LevelRow(
                    level = level,
                    maxVolume = maxVolume,
                    selectedPrice = selectedPrice,
                    bestBid = aggregatedBestBid,
                    bestAsk = aggregatedBestAsk,
                    aggregationLevel = aggregationLevel,
                    baseTickSize = baseTickSize,
                    onPriceClick = onPriceSelected
                )
            }
        }
    }
}

private val symFmt = SymbolFormatter.DEFAULT

fun formatPrice(price: Double): String = symFmt.formatPrice(price)

fun formatVolume(volume: Double): String = symFmt.formatVolume(volume)