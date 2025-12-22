package com.aandios.tradingterminal.ui.chart

import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aandios.tradingterminal.ui.components.TerminalBadge
import com.aandios.tradingterminal.ui.components.TerminalButton
import com.aandios.tradingterminal.ui.components.TerminalCard
import com.aandios.tradingterminal.ui.components.TerminalDivider
import com.aandios.tradingterminal.ui.components.TerminalStatusBar
import com.aandios.tradingterminal.ui.components.TerminalToolbar
import com.aandios.tradingterminal.ui.dom.DomViewModel
import com.aandios.tradingterminal.ui.dom.DomWidget
import com.aandios.tradingterminal.ui.theme.*
import com.aandios.tradingterminal.ui.trades.TradesViewModel
import com.aandios.tradingterminal.ui.trades.TradesWidget
import com.aandios.tradingterminal.utils.formatPrice

@Composable
fun ChartScreen(
    chartViewModel: ChartViewModel,
    domViewModel: DomViewModel,
    tradesViewModel: TradesViewModel,
    modifier: Modifier = Modifier,
) {
    val chartState by chartViewModel.chartState.collectAsState()
    val orderBook by domViewModel.orderBook.collectAsState()
    val selectedPrice by domViewModel.selectedPrice.collectAsState()
    val orderQuantity by domViewModel.orderQuantity.collectAsState()

    // Состояние для выбранного инструмента и таймфрейма
    var selectedSymbol by remember { mutableStateOf("BTCUSDT") }
    var selectedTimeframe by remember { mutableStateOf("1h") }

    // Конфигурация графика
    val chartConfig = remember {
        ChartConfig(
            candleStyle = CandleStyle(
                shadowColor = ChartColors.candleShadow,
                bullishColor = ChartColors.bullish,
                bearishColor = ChartColors.bearish
            )
        )
    }

    LaunchedEffect(selectedSymbol, selectedTimeframe) {
        chartViewModel.loadChart(selectedSymbol, selectedTimeframe)
        domViewModel.subscribeToOrderBook(selectedSymbol)
        tradesViewModel.subscribeToTrades(selectedSymbol)
    }

    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(1.dp) // Минимальные промежутки
    ) {
        // Верхняя панель инструментов
        TerminalToolbar {
            SymbolSelector(selectedSymbol) { symbol ->
                selectedSymbol = symbol
            }

            Spacer(modifier = Modifier.weight(1f))

            TimeframeSelector(selectedTimeframe) { timeframe ->
                selectedTimeframe = timeframe
            }

            TerminalBadge(
                text = "BINANCE", modifier = Modifier.padding(start = 8.dp)
            )
        }

        // Основное содержимое
        TerminalCard(
            modifier = Modifier.fillMaxSize()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                // График - основное пространство
                ChartContent(
                    chartState = chartState,
                    chartConfig = chartConfig,
                    symbol = selectedSymbol,
                    timeframe = selectedTimeframe,
                    modifier = Modifier
                        .fillMaxHeight()
                        .weight(1f) // Растягивается на всё доступное
                )

                // DOM
                DomWidget(
                    orderBook = orderBook,
                    selectedPrice = selectedPrice,
                    onPriceSelected = { price ->
                        domViewModel.selectPrice(price)
                    },
                    orderQuantity = orderQuantity,
                    onQuantityChanged = { quantity ->
                        domViewModel.updateOrderQuantity(quantity)
                    },
                    onPlaceOrder = { side ->
                        domViewModel.placeOrder(side)
                    },
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(300.dp)
                )

                // Trades
                TradesWidget(
                    viewModel = tradesViewModel,
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(250.dp)
                )
            }
        }
        TerminalStatusBar(
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun SymbolSelector(
    selectedSymbol: String, onSymbolSelected: (String) -> Unit
) {
    val symbols = listOf("BTCUSDT", "ETHUSDT","LTCUSDT", "BNBUSDT", "SOLUSDT", "XRPUSDT")

    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        symbols.forEach { symbol ->
            TerminalButton(
                onClick = { onSymbolSelected(symbol) },
                isActive = symbol == selectedSymbol,
                modifier = Modifier.height(32.dp)
            ) {
                Text(
                    text = symbol.replace("USDT", ""), style = MaterialTheme.typography.labelMedium
                )
            }
        }
    }
}

@Composable
private fun TimeframeSelector(
    selectedTimeframe: String, onTimeframeSelected: (String) -> Unit
) {
    val timeframes = listOf("1m", "5m", "15m", "30m", "1h", "4h", "1d", "1w")

    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        timeframes.forEach { tf ->
            TerminalButton(
                onClick = { onTimeframeSelected(tf) },
                isActive = tf == selectedTimeframe,
                modifier = Modifier.height(32.dp)
            ) {
                Text(
                    text = tf, style = MaterialTheme.typography.labelMedium
                )
            }
        }
    }
}

@Composable
private fun ChartContent(
    chartState: ChartState,
    chartConfig: ChartConfig,
    symbol: String,
    timeframe: String,
    modifier: Modifier,
) {
    Box(
        modifier = modifier, contentAlignment = Alignment.Center
    ) {
        when (chartState) {
            is ChartState.Loading -> {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Loading $symbol $timeframe...",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            is ChartState.Success -> {
                val candles = chartState.candles
                val lastPrice = candles.lastOrNull()?.close  // Получаем последнюю цену

                Column(
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Заголовок графика - ОБНОВИЛ
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "$symbol • $timeframe",
                                color = MaterialTheme.colorScheme.onSurface,
                                style = MaterialTheme.typography.titleMedium
                            )

                            // ДОБАВИЛ: Показываем текущую цену большим шрифтом
                            if (lastPrice != null) {
                                Text(
                                    text = formatPrice(lastPrice),
                                    color = MaterialTheme.colorScheme.onSurface,
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Text(
                                text = "${candles.size} candles • Binance",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.labelSmall
                            )
                        }

                        // Статистика
                        if (candles.size >= 2) {
                            val lastCandle = candles.last()
                            val prevCandle = candles[candles.size - 2]
                            val change = ((lastCandle.close - prevCandle.close) / prevCandle.close * 100)
                            val isBullish = change >= 0

                            TerminalBadge(
                                text = "${String.format("%.2f", lastCandle.close)} (${
                                    String.format(
                                        "%+.2f",
                                        change
                                    )
                                }%)",
                                isBullish = isBullish
                            )
                        }
                    }

                    TerminalDivider()

                    // График - ПЕРЕДАЕМ текущую цену
                    CandleStickChart(
                        candles = candles,
                        currentPrice = lastPrice,  // ДОБАВИЛ
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        config = chartConfig
                    )
                }
            }


            is ChartState.Error -> {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "⚠️", style = MaterialTheme.typography.displayMedium
                    )
                    Text(
                        text = "Connection Error",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = (chartState as ChartState.Error).message,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium
                    )

                    TerminalButton(
                        onClick = { /* retry */ }, modifier = Modifier.padding(top = 8.dp)
                    ) {
                        Text("Retry")
                    }
                }
            }
        }
    }
}