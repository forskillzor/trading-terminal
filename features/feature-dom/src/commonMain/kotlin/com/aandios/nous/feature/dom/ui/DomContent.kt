package com.aandios.nous.feature.dom.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aandios.nous_platform.domain.commands.TradingCommand
import com.aandios.nous_platform.domain.entities.OrderBookData
import org.koin.compose.koinInject
import kotlin.math.max

@Composable
fun DomContent(
    orderBook: OrderBookData,
    selectedPrice: Double?,
    onPriceSelected: (Double?) -> Unit,
    orderQuantity: String,
    onQuantityChanged: (String) -> Unit,
    onCreateBuyMarket: () -> TradingCommand,
    onCreateSellMarket: () -> TradingCommand,
    onCreateBuyLimit: (() -> TradingCommand)?,
    onCreateSellLimit: (() -> TradingCommand)?,
    onCreateBuyBestBid: (() -> TradingCommand)?,
    onCreateSellBestAsk: (() -> TradingCommand)?,
    onCreateTradeOff: () -> TradingCommand,
    onExecuteCommand: (TradingCommand?) -> Unit,
    isTradingEnabled: Boolean,
    modifier: Modifier = Modifier
) {
    val domViewModel: DomViewModel = koinInject()
    val bestPrices by domViewModel.bestPrices.collectAsState()
    Column(
        modifier = modifier.fillMaxSize()
    ) {
        val maxVolume = remember(orderBook.bids, orderBook.asks) {
            max(
                orderBook.bids.maxOfOrNull { it.quantity.toDouble() } ?: 0.0,
                orderBook.asks.maxOfOrNull { it.quantity.toDouble() } ?: 0.0
            )
        }
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
        Column(
        ) {

            DomSection(
                levels = orderBook.asks,
                maxVolume = maxVolume,
                isAsk = true,
                selectedPrice = selectedPrice,
                onPriceClick = { price -> onPriceSelected(price) },
                modifier = Modifier.weight(1f)
            )

            // Spread (разница)
            DomSpread(
                bestPrices = bestPrices,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)  // Чуть выше для отображения объемов
            )
            // BIDS (покупки - зеленые)
            DomSection(
                levels = orderBook.bids,
                maxVolume = maxVolume,
                isAsk = false,
                selectedPrice = selectedPrice,
                onPriceClick = { price -> onPriceSelected(price) },
                modifier = Modifier.weight(1f)
            )
        }

        // Панель размещения ордера
        OrderPlacementPanel(
            selectedPrice = selectedPrice,
            orderQuantity = orderQuantity,
            onQuantityChanged = onQuantityChanged,
            onCreateBuyMarket = onCreateBuyMarket,
            onCreateSellMarket = onCreateSellMarket,
            onCreateBuyLimit = onCreateBuyLimit,
            onCreateSellLimit = onCreateSellLimit,
            onCreateBuyBestBid = onCreateBuyBestBid,
            onCreateSellBestAsk = onCreateSellBestAsk,
            onCreateTradeOff = onCreateTradeOff,
            onExecuteCommand = onExecuteCommand,
            isTradingEnabled = isTradingEnabled,
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)  // Высота под все кнопки
        )
    }
}