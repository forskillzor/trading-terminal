package com.aandios.nous_platform.ui.trades


import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aandios.nous_platform.data.api.binance.models.Trade
import com.aandios.nous_platform.data.api.binance.models.TradeSide

@Composable
fun TradesWidget(
    viewModel: TradesViewModel,
    modifier: Modifier = Modifier,
    width: Dp = 250.dp,
    showHeader: Boolean = true
) {
    val trades by viewModel.trades.collectAsState()

    Column(
        modifier = modifier
            .width(width)
            .fillMaxHeight()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        if (showHeader) {
            TradesHeader(
                modifier = Modifier.fillMaxWidth()
            )
        }

        TradesList(
            trades = trades,
            viewModel = viewModel,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun TradesHeader(
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = modifier
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Time",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.weight(1.2f)
            )
            Text(
                text = "Price",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.weight(1.5f)
            )
            Text(
                text = "Qty",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun TradesList(
    trades: List<Trade>,
    viewModel: TradesViewModel,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()

    // Автопрокрутка к началу при добавлении новых сделок
    LaunchedEffect(trades.size) {
        if (trades.isNotEmpty() && listState.firstVisibleItemIndex < 5) {
            listState.scrollToItem(0)
        }
    }

    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Top,
        reverseLayout = false // Новые сделки вверху
    ) {
        itemsIndexed(trades) { index, trade ->
            TradeRow(
                trade = trade,
                viewModel = viewModel,
                index = index,
                modifier = Modifier.fillMaxWidth()
            )

            if (index < trades.size - 1) {
                Divider(
                    modifier = Modifier.padding(horizontal = 4.dp),
                    color = MaterialTheme.colorScheme.outlineVariant,
                    thickness = 0.5.dp
                )
            }
        }
    }
}

@Composable
private fun TradeRow(
    trade: Trade,
    viewModel: TradesViewModel,
    index: Int,
    modifier: Modifier = Modifier
) {
    val backgroundColor = if (index % 2 == 0) {
        MaterialTheme.colorScheme.surface
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
    }

    val textColor = when (trade.side) {
        TradeSide.BUY -> MaterialTheme.colorScheme.primary // Зеленый
        TradeSide.SELL -> MaterialTheme.colorScheme.secondary // Красный
    }

    Surface(
        color = backgroundColor,
        modifier = modifier
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 0.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Время
            Text(
                text = viewModel.formatTime(trade.timestamp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                ),
                modifier = Modifier.weight(1.2f)
            )

            // Цена
            Text(
                text = viewModel.formatPrice(trade.price),
                color = textColor,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = if (trade.quantity > 10) MaterialTheme.typography.labelSmall.fontWeight else null
                ),
                modifier = Modifier.weight(1.5f)
            )

            // Объем
            Text(
                text = viewModel.formatQuantity(trade.quantity),
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                ),
                modifier = Modifier.weight(1f)
            )
        }
    }
}