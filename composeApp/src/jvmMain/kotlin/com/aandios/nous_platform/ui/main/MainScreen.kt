package com.aandios.nous_platform.ui.main


import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aandios.nous.feature.trades.ui.TradesViewModel
import com.aandios.nous.feature.trades.ui.TradesWidget
import com.aandios.nous_platform.ui.chart.*
import com.aandios.nous_platform.ui.components.*
import com.aandios.nous_platform.ui.dom.DomViewModel
import com.aandios.nous_platform.ui.dom.DomWidget
import com.aandios.nous_platform.ui.theme.ChartColors
//import com.aandios.nous.feature.trades.ui.TradesViewModel
//import com.aandios.nous.feature.trades.ui.TradesWidget
import com.aandios.nous_platform.utils.formatPrice

@Composable
fun MainScreen(
    chartViewModel: ChartViewModel,
    domViewModel: DomViewModel,
    tradesViewModel: TradesViewModel,
    modifier: Modifier = Modifier,
) {
    val chartState by chartViewModel.chartState.collectAsState()
    val orderBook by domViewModel.orderBook.collectAsState()
    val bestPrices by domViewModel.bestPrices.collectAsState()
    val selectedPrice by domViewModel.selectedPrice.collectAsState()
    val orderQuantity by domViewModel.orderQuantity.collectAsState()

    var selectedSymbol by remember { mutableStateOf("BTCUSDT") }
    var selectedTimeframe by remember { mutableStateOf("1h") }

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
        verticalArrangement = Arrangement.spacedBy(1.dp)
    ) {

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

        TerminalCard(
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                ChartWidget(
                    chartState = chartState,
                    chartConfig = chartConfig,
                    symbol = selectedSymbol,
                    timeframe = selectedTimeframe,
                    modifier = Modifier
                        .fillMaxHeight()
                        .weight(1f)
                )

                // DOM
                DomWidget(
                    orderBook = orderBook,
                    bestPrices = bestPrices,
                    selectedPrice = selectedPrice,
                    onPriceSelected = { price -> domViewModel.selectPrice(price) },
                    orderQuantity = orderQuantity,
                    onQuantityChanged = { quantity -> domViewModel.updateOrderQuantity(quantity) },
                    onTradingCommand = { command -> domViewModel.executeCommand(command) },
                    onCommandResult = domViewModel::onCommandResult,
                    isTradingEnabled = domViewModel.isTradingEnabled.collectAsState().value,
                    modifier = Modifier.width(300.dp)
                )

                // Trades
                TradesWidget(
                    viewModel = tradesViewModel,
                    modifier = Modifier
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
    val symbols = listOf("BTCUSDT", "ETHUSDT", "LTCUSDT", "BNBUSDT", "SOLUSDT", "XRPUSDT")

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
private fun ChartWidget(
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
                val lastPrice = candles.lastOrNull()?.close

                Column(
                ) {
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

                    CandleStickChart(
                        candles = candles,
                        currentPrice = lastPrice,  // ДОБАВИЛ
                        modifier = Modifier
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
                        text = chartState.message,
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