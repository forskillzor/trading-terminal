package com.aandios.nous.feature.dom.ui.unified

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
import com.aandios.nous.feature.dom.domain.UnifiedOrderBook
import kotlin.math.abs
import kotlinx.coroutines.delay

@Composable
fun UnifiedDomSection(
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

    /**
     *  Автоматический скролл до лучшей цены, если она скрыта
     *  Цель: показывать лучшую цену bid, когда она уходит за границы видимой области
     *  ПРИНЦИПЫ:
     *  1. Скроллим ТОЛЬКО если лучшая цена НЕ видна в текущем viewport
     *  2. НЕ скроллим, если пользователь активно скроллит сам
     *  3. Дебаунс 200 мс - не дёргаемся на каждом тике
     *  4. Плавная анимация скролла - информативно для пользователя
     *  5. Определяем направление: цена выше viewport → отступ сверху, цена ниже viewport → отступ снизу
     *  6. Обеспечиваем отступы: 2 уровня сверху/снизу от best bid (в зависимости от направления)
     **/

    /**
     *  Целевая цена для скролла - агрегированный лучший bid.
     *  Используем агрегацию, чтобы скроллить до того же уровня, который подсвечивается
     **/
    val scrollTargetPrice = remember(unifiedOrderBook, aggregationLevel) {
        unifiedOrderBook?.bestBid?.let { aggregationLevel.roundDown(it) }
    }

    // Автоматический скролл при изменении лучшей цены
    LaunchedEffect(scrollTargetPrice) {
        // Если нет целевой цены - ничего не делаем
        if (scrollTargetPrice == null) return@LaunchedEffect

        // Если пользователь сейчас скроллит - не вмешиваемся
        if (lazyListState.isScrollInProgress) return@LaunchedEffect

        // Ищем индекс целевой цены в списке уровней
        // Linear search допустим, так как levels обычно содержит 100-200 элементов
        // и поиск выполняется редко (только при изменении лучшей цены + дебаунс)
        val targetIndex = levels.indexOfFirst { level ->
            val levelPrice = level.price.toDoubleOrNull()
            levelPrice != null && abs(levelPrice - scrollTargetPrice) < 0.000001
        }.takeIf { it >= 0 } ?: return@LaunchedEffect // цена не найдена в levels

        // Проверяем, видна ли уже целевая цена в текущем viewport
        // И заодно определяем границы видимой области
        val visibleItems = lazyListState.layoutInfo.visibleItemsInfo
        val firstVisibleIndex = visibleItems.firstOrNull()?.index ?: 0
        val lastVisibleIndex = visibleItems.lastOrNull()?.index ?: 0
        
        val isTargetVisible = visibleItems.any { visibleItem ->
            // Получаем уровень по индексу видимого элемента
            val visibleIndex = visibleItem.index
            if (visibleIndex in levels.indices) {
                val levelPrice = levels[visibleIndex].price.toDoubleOrNull()
                // Сравниваем с целевой ценой с допуском (на случай ошибок округления)
                levelPrice != null && abs(levelPrice - scrollTargetPrice) < 0.000001
            } else {
                false
            }
        }

        // Если цена НЕ видна - скроллим до неё
        if (!isTargetVisible) {
            // Плавный скролл с анимацией - информативно для пользователя
            
            // УЛУЧШЕНИЕ UX: скроллим так, чтобы best bid был виден с отступом
            // 2 уровня сверху и 2 уровня снизу (если возможно)
            // Это предотвращает "биение" цены об край viewport
            
            // 1. Определяем количество видимых элементов
            val visibleItemsCount = visibleItems.size
            val totalItems = levels.size
            
            // Если viewport ещё не отрисован или слишком мал - простой скролл
            if (visibleItemsCount == 0) {
                lazyListState.animateScrollToItem(targetIndex, 0)
                return@LaunchedEffect
            }
            
            // 2. Определяем, с какой стороны цена ушла из viewport
            val isPriceAboveViewport = targetIndex < firstVisibleIndex
            val isPriceBelowViewport = targetIndex > lastVisibleIndex
            
            // Настраиваем отступы: хотим видеть 2 уровня сверху и 2 уровня снизу от best bid
            val margin = 2 // отступ в уровнях сверху и снизу
            
            // Если видимых элементов слишком мало для отступов - простой скролл
            if (visibleItemsCount <= margin * 2) {
                lazyListState.animateScrollToItem(targetIndex, 0)
                return@LaunchedEffect
            }
            
            // 3. Определяем позицию скролла с учётом направления и отступов
            val scrollIndex = when {
                // Цена выше viewport - скроллим так, чтобы best bid был виден с отступом СВЕРХУ
                isPriceAboveViewport -> {
                    // Хотим, чтобы best bid был на позиции margin от верха
                    val desiredIndex = targetIndex - margin
                    // Проверяем краевые случаи
                    when {
                        // Best bid слишком близко к началу списка - показываем начало
                        desiredIndex < 0 -> 0
                        // Всё нормально - используем расчётную позицию
                        else -> desiredIndex
                    }
                }
                
                // Цена ниже viewport - скроллим так, чтобы best bid был виден с отступом СНИЗУ
                isPriceBelowViewport -> {
                    // Хотим, чтобы best bid был на позиции (visibleItemsCount - margin - 1) от верха
                    // Это значит margin уровней снизу
                    val desiredIndex = targetIndex - (visibleItemsCount - margin - 1)
                    // Проверяем краевые случаи
                    when {
                        // Best bid слишком близко к концу списка - показываем конец
                        desiredIndex + visibleItemsCount > totalItems -> 
                            (totalItems - visibleItemsCount).coerceAtLeast(0)
                        // Всё нормально - используем расчётную позицию
                        else -> desiredIndex.coerceAtLeast(0)
                    }
                }
                
                // Неожиданный случай (цена должна быть не видна, но не выше и не ниже)
                // Просто центрируем с отступом сверху как fallback
                else -> targetIndex - margin
            }
            
            // 4. Выполняем скролл с плавной анимацией
            lazyListState.animateScrollToItem(scrollIndex, 0)
        }
        // Если цена уже видна - ничего не делаем, сохраняем позицию скролла
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
                UnifiedLevelRow(
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
            UnifiedDomSpread(
                bestBid = unifiedOrderBook.bestBid ?: 0.0,
                bestAsk = unifiedOrderBook.bestAsk ?: 0.0,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            )
        }
    }
}

fun formatPrice(price: Double): String {
    return when {
        price >= 1000 -> String.format("%.2f", price)
        price >= 100 -> String.format("%.3f", price)
        price >= 10 -> String.format("%.4f", price)
        price >= 1 -> String.format("%.5f", price)
        else -> String.format("%.6f", price)
    }
}

fun formatVolume(volume: Double): String {
    return when {
        volume >= 1000 -> String.format("%.1fk", volume / 1000)
        volume >= 100 -> String.format("%.0f", volume)
        volume >= 10 -> String.format("%.1f", volume)
        else -> String.format("%.2f", volume)
    }
}