package com.aandios.nous.feature.dom.ui.split

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.aandios.nous.api.market.model.orderbook.OrderBookLevel
import com.aandios.nous.feature.dom.domain.model.AggregationLevel

@Composable
fun SplitDomSection(
    levels: List<OrderBookLevel>,
    maxVolume: Double,
    isAsk: Boolean,
    selectedPrice: Double?,
    baseTickSize: Double? = null,
    onPriceClick: (Double) -> Unit,
    listState: LazyListState? = null,
    modifier: Modifier = Modifier
) {

    // Список уровней с оптимизациями
    LazyColumn(
        state = listState ?: rememberLazyListState(),
        modifier = modifier.fillMaxSize(),
    ) {
        items(
            items = levels,
            key = { it.price } // Ключ по цене - стабильный!
        ) { level ->
            val price = level.price.toDoubleOrNull() ?: return@items
            val isSelected = selectedPrice?.let { selected ->
                if (baseTickSize != null) {
                    AggregationLevel.BaseTick.aggregationKey(price.toString(), baseTickSize) ==
                        AggregationLevel.BaseTick.aggregationKey(selected.toString(), baseTickSize)
                } else {
                    // Fallback для обратной совместимости
                    kotlin.math.abs(selected - price) < 0.000001
                }
            } ?: false

            SplitLevelRow(
                level = level,
                isAsk = isAsk,
                isSelected = isSelected,
                maxVolume = maxVolume,
                totalMax = maxVolume,
                onPriceClick = { onPriceClick(price) }
            )
        }
    }
}

