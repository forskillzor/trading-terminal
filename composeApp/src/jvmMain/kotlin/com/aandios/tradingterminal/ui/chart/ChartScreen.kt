package com.aandios.tradingterminal.ui.chart

import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.aandios.tradingterminal.ui.components.TerminalStatusBar
import com.aandios.tradingterminal.ui.theme.*
import com.aandios.tradingterminal.ui.theme.TerminalBadge
import com.aandios.tradingterminal.ui.theme.TerminalButton
import com.aandios.tradingterminal.ui.theme.TerminalCard
import com.aandios.tradingterminal.ui.theme.TerminalDivider
import com.aandios.tradingterminal.ui.theme.TerminalToolbar

@Composable
fun ChartScreen(
    viewModel: ChartViewModel, modifier: Modifier = Modifier
) {
    val chartState by viewModel.chartState.collectAsState()

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
        viewModel.loadChart(selectedSymbol, selectedTimeframe)
    }

    Column(
        modifier = modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(1.dp) // Минимальные промежутки
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
            ChartContent(
                chartState = chartState,
                chartConfig = chartConfig,
                symbol = selectedSymbol,
                timeframe = selectedTimeframe
            )
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
    val symbols = listOf("BTCUSDT", "ETHUSDT", "BNBUSDT", "SOLUSDT", "XRPUSDT")

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
    chartState: ChartState, chartConfig: ChartConfig, symbol: String, timeframe: String
) {
    Box(
        modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center
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
                val candles = (chartState as ChartState.Success).candles

                Column(
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Заголовок графика
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "$symbol • $timeframe",
                                color = MaterialTheme.colorScheme.onSurface,
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = "${candles.size} candles • Binance",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.labelSmall
                            )
                        }

                        // Статистика
                        if (candles.isNotEmpty()) {
                            val lastCandle = candles.last()
                            val change = ((lastCandle.close - lastCandle.open) / lastCandle.open * 100)
                            val isBullish = change >= 0

                            TerminalBadge(
                                text = "${String.format("%.2f", lastCandle.close)} (${String.format("%.2f", change)}%)",
                                isBullish = isBullish
                            )
                        }
                    }

                    TerminalDivider()

                    // График
                    CandleStickChart(
                        candles = candles, modifier = Modifier.fillMaxSize().padding(16.dp), config = chartConfig
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