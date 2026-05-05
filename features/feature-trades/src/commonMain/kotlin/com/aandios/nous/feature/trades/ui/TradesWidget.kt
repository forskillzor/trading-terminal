package com.aandios.nous.feature.trades.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aandios.nous.api.market.model.SymbolInfo
import com.aandios.nous.api.market.model.trades.Trade
import com.aandios.nous.feature.trades.ui.header.SizeFilterDropdown
import com.aandios.nous.feature.trades.ui.header.TradesSymbolDropdown

/**
 * Панель для отображения потока сделок (Trades).
 * Использует MaterialTheme цвета (как feature-dom) + кастомный header с dropdowns.
 */
@Composable
fun TradesWidget(
    viewModel: TradesViewModel,
    currentSymbol: String,
    onSymbolChanged: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsState()
    val loadedSymbols by viewModel.loadedSymbols.collectAsState()
    val currentSymbolInfo by viewModel.currentSymbolInfo.collectAsState()
    val selectedSizeFilter by viewModel.selectedSizeFilter.collectAsState()

    Column(modifier = modifier.background(MaterialTheme.colorScheme.background)) {
        // Header bar с dropdowns (как в feature-dom)
        TradesHeaderBar(
            currentSymbol = currentSymbol,
            availableSymbols = loadedSymbols,
            currentSymbolInfo = currentSymbolInfo,
            selectedSizeFilter = selectedSizeFilter,
            onSymbolChanged = onSymbolChanged,
            onSizeFilterChanged = { viewModel.updateSizeFilter(it) },
        )

        // Заголовки колонок
        ColumnHeaderRow()

        HorizontalDivider(
            thickness = 1.dp,
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
        )

        // Контент
        when (val currentState = state) {
            is TradesState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Подключение...",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp
                    )
                }
            }
            is TradesState.Error -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = currentState.message,
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 13.sp
                    )
                }
            }
            is TradesState.Connected -> {
                val allTrades = currentState.trades
                val filteredTrades = viewModel.getFilteredTrades(allTrades)

                if (filteredTrades.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (selectedSizeFilter != SizeFilter.All) "Нет сделок > фильтра" else "Ожидание данных...",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 13.sp
                        )
                    }
                } else {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(filteredTrades, key = { it.id }) { trade ->
                            TradeRow(
                                trade = trade,
                                viewModel = viewModel,
                                index = filteredTrades.indexOf(trade)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Верхняя панель с symbol dropdown и size filter dropdown (как в DomHeader).
 */
@Composable
private fun TradesHeaderBar(
    currentSymbol: String,
    availableSymbols: List<SymbolInfo>,
    currentSymbolInfo: SymbolInfo?,
    selectedSizeFilter: SizeFilter,
    onSymbolChanged: (String) -> Unit,
    onSizeFilterChanged: (SizeFilter) -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 1.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Symbol dropdown
            TradesSymbolDropdown(
                currentSymbol = currentSymbol,
                availableSymbols = availableSymbols,
                onSymbolChanged = onSymbolChanged,
                modifier = Modifier.weight(1.4f)
            )

            // Size filter dropdown
            SizeFilterDropdown(
                currentFilter = selectedSizeFilter,
                minQty = currentSymbolInfo?.minQty,
                onFilterChanged = onSizeFilterChanged,
                modifier = Modifier.weight(1f)
            )

            // Live индикатор
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(
                        color = Color.Green,
                        shape = MaterialTheme.shapes.small
                    )
            )
        }
    }
}

/**
 * Строка заголовков колонок: Время / Цена / Кол-во.
 */
@Composable
private fun ColumnHeaderRow() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(horizontal = 8.dp, vertical = 5.dp)
    ) {
        Text(
            text = "Время",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1.2f)
        )
        Text(
            text = "Цена",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = "Кол-во",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(0.8f)
        )
    }
}

/**
 * Строка одной сделки.
 * Цвета: buy = MaterialTheme.colorScheme.primary, sell = MaterialTheme.colorScheme.secondary.
 */
@Composable
private fun TradeRow(
    trade: Trade,
    viewModel: TradesViewModel,
    index: Int
) {
    val bgColor = if (index % 2 == 0) Color.Transparent
    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)
    val priceColor = if (trade.isBuyerMaker) MaterialTheme.colorScheme.secondary  // продажа (красный)
    else MaterialTheme.colorScheme.primary  // покупка (зелёный)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(bgColor)
            .padding(horizontal = 8.dp, vertical = 0.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Время
        Text(
            text = viewModel.formatTime(trade.timestamp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 11.sp,
            modifier = Modifier.weight(1.2f)
        )

        // Цена
        Text(
            text = viewModel.formatPrice(trade.price),
            color = priceColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1f)
        )

        // Количество
        Text(
            text = viewModel.formatQuantity(trade.quantity),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 11.sp,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(0.8f)
        )
    }
}
