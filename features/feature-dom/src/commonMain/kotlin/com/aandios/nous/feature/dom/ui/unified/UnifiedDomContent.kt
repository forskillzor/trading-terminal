package com.aandios.nous.feature.dom.ui.unified

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.aandios.nous.feature.dom.domain.model.AggregationLevel
import com.aandios.nous.feature.dom.domain.UnifiedOrderBook

@Composable
fun UnifiedDomContent(
    unifiedOrderBook: UnifiedOrderBook,
    aggregationLevel: AggregationLevel = AggregationLevel.BaseTick,
    baseTickSize: Double? = null,
    selectedPrice: Double? = null,
    onPriceSelected: (Double?) -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize()) {
        UnifiedDomSection(
            unifiedOrderBook = unifiedOrderBook,
            selectedPrice = selectedPrice,
            onPriceSelected = { price -> onPriceSelected(price) },
            aggregationLevel = aggregationLevel,
            baseTickSize = baseTickSize,
            modifier = Modifier.weight(1f)
        )
    }
}