package com.aandios.nous.feature.dom.ui.split

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aandios.nous.api.market.model.BookTicker
import com.aandios.nous.api.market.model.orderbook.OrderBook
import com.aandios.nous.feature.dom.ui.DomSpread
import kotlin.math.max

@Composable
fun SplitDomContent(
    orderBook: OrderBook,
    bookTicker: BookTicker?,
    selectedPrice: Double?,
    baseTickSize: Double? = null,
    onPriceSelected: (Double?) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxSize()
    ) {
        val maxVolume = remember(orderBook.bids, orderBook.asks) {
            max(
                orderBook.bids.maxOfOrNull { it.quantity.toDouble() } ?: 0.0,
                orderBook.asks.maxOfOrNull { it.quantity.toDouble() } ?: 0.0
            )
        }
        
        // Заголовки столбцов
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
        
        Column {
            // ASK (продажи - красные)
            SplitDomSection(
                levels = orderBook.asks,
                maxVolume = maxVolume,
                isAsk = true,
                selectedPrice = selectedPrice,
                baseTickSize = baseTickSize,
                onPriceClick = { price -> onPriceSelected(price) },
                modifier = Modifier.weight(1f)
            )
            
            // Spread (разница)
            DomSpread(
                bookTicker = bookTicker,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            )
            
            // BID (покупки - зеленые)
            SplitDomSection(
                levels = orderBook.bids,
                maxVolume = maxVolume,
                isAsk = false,
                selectedPrice = selectedPrice,
                baseTickSize = baseTickSize,
                onPriceClick = { price -> onPriceSelected(price) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}