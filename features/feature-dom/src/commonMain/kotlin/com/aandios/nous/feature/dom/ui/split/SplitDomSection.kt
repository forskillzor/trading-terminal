package com.aandios.nous.feature.dom.ui.split

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.aandios.nous.api.market.model.orderbook.OrderBookLevel
import kotlin.math.abs

@Composable
fun SplitDomSection(
    levels: List<OrderBookLevel>,
    maxVolume: Double,
    isAsk: Boolean,
    selectedPrice: Double?,
    onPriceClick: (Double) -> Unit,
    modifier: Modifier = Modifier
) {

    // Список уровней с оптимизациями
    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
    ) {
        items(
            items = levels,
            key = { it.price } // Ключ по цене - стабильный!
        ) { level ->
            val price = level.price.toDoubleOrNull() ?: return@items
            val isSelected = selectedPrice?.let { abs(it - price) < 0.000001 } ?: false

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

