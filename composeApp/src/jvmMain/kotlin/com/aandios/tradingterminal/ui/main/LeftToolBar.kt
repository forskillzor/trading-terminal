package com.aandios.tradingterminal.ui.main

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.Divider
import androidx.compose.material3.FilterChip
import androidx.compose.material.IconButton
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aandios.tradingterminal.data.api.binance.models.TradeSide
import com.aandios.tradingterminal.ui.components.TerminalButton
import com.aandios.tradingterminal.utils.formatTime
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import tradingterminal.composeapp.generated.resources.Res
import tradingterminal.composeapp.generated.resources.candlestick
import tradingterminal.composeapp.generated.resources.clock
import tradingterminal.composeapp.generated.resources.close
import tradingterminal.composeapp.generated.resources.code
import tradingterminal.composeapp.generated.resources.icon_plus
import tradingterminal.composeapp.generated.resources.indicators
import tradingterminal.composeapp.generated.resources.pencil
import tradingterminal.composeapp.generated.resources.robot
import tradingterminal.composeapp.generated.resources.send
import tradingterminal.composeapp.generated.resources.terminal
import tradingterminal.composeapp.generated.resources.trash
import tradingterminal.composeapp.generated.resources.wallet
import java.text.SimpleDateFormat
import java.util.Date

// Сначала создадим enum для типов инструментов
enum class ToolPanelType {
    SYMBOLS,
    INDICATORS,
    TIMEFRAMES,
    DRAWINGS,
    STRATEGIES
}

data class ToolPanelState(
    val isExpanded: Boolean = false,
    val type: ToolPanelType? = null,
    val width: Dp = 200.dp
)

enum class BottomToolType {
    PORTFOLIO,
    CONSOLE,
    EDITOR
}

enum class PortfolioTab {
    POSITIONS,
    ORDERS,
    BALANCE,
    STATS
}

// Мок данные для портфеля
data class MockPosition(
    val symbol: String,
    val side: TradeSide,
    val quantity: Double,
    val entryPrice: Double,
    val currentPrice: Double,
    val pnl: Double,
    val pnlPercent: Double
)

data class MockOrder(
    val id: String,
    val symbol: String,
    val side: TradeSide,
    val type: String, // "LIMIT" or "MARKET"
    val price: Double,
    val quantity: Double,
    val filled: Double,
    val timestamp: Long,
    val status: String // "OPEN", "FILLED", "CANCELLED"
)

data class MockBalance(
    val asset: String,
    val free: Double,
    val locked: Double,
    val total: Double,
    val usdValue: Double
)

@Composable
fun LeftToolBar(
    selectedSymbol: String,
    onSymbolSelected: (String) -> Unit,
    selectedTimeframe: String,
    onTimeframeSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var topPanelState by remember { mutableStateOf(ToolPanelState()) }
    var bottomPanelState by remember { mutableStateOf<BottomToolType?>(null) }

    // Используем Row для размещения основной панели и выдвижных панелей рядом
    Row(
        modifier = modifier.fillMaxHeight()
    ) {
        // Основная панель с иконками (всегда видима)
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .width(48.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            // Верхняя группа иконок
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                ToolBarIcon(
                    icon = Res.drawable.candlestick,
                    description = "Symbols",
                    isSelected = topPanelState.type == ToolPanelType.SYMBOLS && topPanelState.isExpanded,
                    onClick = {
                        topPanelState = if (topPanelState.type == ToolPanelType.SYMBOLS) {
                            topPanelState.copy(isExpanded = !topPanelState.isExpanded)
                        } else {
                            topPanelState.copy(isExpanded = true, type = ToolPanelType.SYMBOLS)
                        }
                    }
                )

                ToolBarIcon(
                    icon = Res.drawable.indicators,
                    description = "Indicators",
                    isSelected = topPanelState.type == ToolPanelType.INDICATORS && topPanelState.isExpanded,
                    onClick = {
                        topPanelState = if (topPanelState.type == ToolPanelType.INDICATORS) {
                            topPanelState.copy(isExpanded = !topPanelState.isExpanded)
                        } else {
                            topPanelState.copy(isExpanded = true, type = ToolPanelType.INDICATORS)
                        }
                    }
                )

                ToolBarIcon(
                    icon = Res.drawable.clock,
                    description = "Timeframes",
                    isSelected = topPanelState.type == ToolPanelType.TIMEFRAMES && topPanelState.isExpanded,
                    onClick = {
                        topPanelState = if (topPanelState.type == ToolPanelType.TIMEFRAMES) {
                            topPanelState.copy(isExpanded = !topPanelState.isExpanded)
                        } else {
                            topPanelState.copy(isExpanded = true, type = ToolPanelType.TIMEFRAMES)
                        }
                    }
                )

                ToolBarIcon(
                    icon = Res.drawable.pencil,
                    description = "Drawings",
                    isSelected = topPanelState.type == ToolPanelType.DRAWINGS && topPanelState.isExpanded,
                    onClick = {
                        topPanelState = if (topPanelState.type == ToolPanelType.DRAWINGS) {
                            topPanelState.copy(isExpanded = !topPanelState.isExpanded)
                        } else {
                            topPanelState.copy(isExpanded = true, type = ToolPanelType.DRAWINGS)
                        }
                    }
                )

                ToolBarIcon(
                    icon = Res.drawable.robot,
                    description = "Strategies",
                    isSelected = topPanelState.type == ToolPanelType.STRATEGIES && topPanelState.isExpanded,
                    onClick = {
                        topPanelState = if (topPanelState.type == ToolPanelType.STRATEGIES) {
                            topPanelState.copy(isExpanded = !topPanelState.isExpanded)
                        } else {
                            topPanelState.copy(isExpanded = true, type = ToolPanelType.STRATEGIES)
                        }
                    }
                )
            }

            // Разделитель
            Divider(
                color = MaterialTheme.colorScheme.outline,
                thickness = 1.dp,
                modifier = Modifier.padding(horizontal = 8.dp)
            )

            // Нижняя группа иконок
            Column(
                modifier = Modifier.padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                ToolBarIcon(
                    icon = Res.drawable.wallet,
                    description = "Portfolio",
                    isSelected = bottomPanelState == BottomToolType.PORTFOLIO,
                    onClick = {
                        bottomPanelState = if (bottomPanelState == BottomToolType.PORTFOLIO) null
                        else BottomToolType.PORTFOLIO
                    }
                )

                ToolBarIcon(
                    icon = Res.drawable.terminal,
                    description = "Console",
                    isSelected = bottomPanelState == BottomToolType.CONSOLE,
                    onClick = {
                        bottomPanelState = if (bottomPanelState == BottomToolType.CONSOLE) null
                        else BottomToolType.CONSOLE
                    }
                )

                ToolBarIcon(
                    icon = Res.drawable.code,
                    description = "Editor",
                    isSelected = bottomPanelState == BottomToolType.EDITOR,
                    onClick = {
                        bottomPanelState = if (bottomPanelState == BottomToolType.EDITOR) null
                        else BottomToolType.EDITOR
                    }
                )
            }
        }

        // Верхняя выдвижная панель (слева, под иконками)
        if (topPanelState.isExpanded) {
            ToolDetailsPanel(
                type = topPanelState.type ?: ToolPanelType.SYMBOLS,
                width = topPanelState.width,
                selectedSymbol = selectedSymbol,
                onSymbolSelected = onSymbolSelected,
                selectedTimeframe = selectedTimeframe,
                onTimeframeSelected = onTimeframeSelected,
                onClose = { topPanelState = topPanelState.copy(isExpanded = false) },
                modifier = Modifier
                    .fillMaxHeight()
//                    .align(Alignment.TopStart)
            )
        }

        // Нижняя выдвижная панель (снизу на всю ширину)
        if (bottomPanelState != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
//                    .align(Alignment.BottomCenter)
                    .background(MaterialTheme.colorScheme.surface)
                    .shadow(8.dp)
            ) {
                BottomToolPanel(
                    type = bottomPanelState!!,
                    width = Dp.Unspecified,
                    onClose = { bottomPanelState = null },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

// Остальные функции остаются без изменений...
@Composable
private fun ToolBarIcon(
    icon: DrawableResource,
    description: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()

    Box(
        modifier = modifier
            .size(36.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(
                color = when {
                    isSelected -> MaterialTheme.colorScheme.primary
                    isHovered -> MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                    else -> Color.Transparent
                }
            )
            .hoverable(interactionSource)
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = description,
            modifier = Modifier.size(20.dp),
            tint = if (isSelected) MaterialTheme.colorScheme.onPrimary
            else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ToolDetailsPanel(
    type: ToolPanelType,
    width: Dp,
    selectedSymbol: String,
    onSymbolSelected: (String) -> Unit,
    selectedTimeframe: String,
    onTimeframeSelected: (String) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.width(width),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Заголовок панели
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = when (type) {
                        ToolPanelType.SYMBOLS -> "Symbols"
                        ToolPanelType.INDICATORS -> "Indicators"
                        ToolPanelType.TIMEFRAMES -> "Timeframes"
                        ToolPanelType.DRAWINGS -> "Drawing Tools"
                        ToolPanelType.STRATEGIES -> "Strategies"
                    },
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )

                IconButton(
                    onClick = onClose,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        painter = painterResource(Res.drawable.close), // Замени на свою иконку
                        contentDescription = "Close",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Divider()

            // Контент в зависимости от типа
            when (type) {
                ToolPanelType.SYMBOLS -> {
                    SymbolsPanel(
                        selectedSymbol = selectedSymbol,
                        onSymbolSelected = onSymbolSelected,
                        modifier = Modifier.weight(1f)
                    )
                }

                ToolPanelType.INDICATORS -> {
                    IndicatorsPanel(modifier = Modifier.weight(1f))
                }

                ToolPanelType.TIMEFRAMES -> {
                    TimeframesPanel(
                        selectedTimeframe = selectedTimeframe,
                        onTimeframeSelected = onTimeframeSelected,
                        modifier = Modifier.weight(1f)
                    )
                }

                ToolPanelType.DRAWINGS -> {
                    DrawingsPanel(modifier = Modifier.weight(1f))
                }

                ToolPanelType.STRATEGIES -> {
                    StrategiesPanel(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun SymbolsPanel(
    selectedSymbol: String,
    onSymbolSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val symbols = listOf(
        "BTCUSDT", "ETHUSDT", "BNBUSDT", "SOLUSDT", "XRPUSDT",
        "ADAUSDT", "DOGEUSDT", "DOTUSDT", "LINKUSDT", "LTCUSDT",
        "BCHUSDT", "ALGOUSDT", "MATICUSDT", "UNIUSDT", "ATOMUSDT"
    )

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 4.dp)
    ) {
        items(symbols) { symbol ->
            val isSelected = symbol == selectedSymbol
            val displayName = symbol.replace("USDT", "")

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSymbolSelected(symbol) },
                color = if (isSelected)
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                else Color.Transparent
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = displayName,
                        color = if (isSelected)
                            MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.bodyMedium
                    )

                    // Можно добавить иконку избранного или текущую цену
                    Text(
                        text = "USDT",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        }
    }
}

@Composable
private fun TimeframesPanel(
    selectedTimeframe: String,
    onTimeframeSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val timeframes = listOf(
        "1m" to "1 minute",
        "5m" to "5 minutes",
        "15m" to "15 minutes",
        "30m" to "30 minutes",
        "1h" to "1 hour",
        "4h" to "4 hours",
        "1d" to "1 day",
        "1w" to "1 week"
    )

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 4.dp)
    ) {
        items(timeframes) { (tf, description) ->
            val isSelected = tf == selectedTimeframe

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onTimeframeSelected(tf) },
                color = if (isSelected)
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                else Color.Transparent
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = tf,
                        color = if (isSelected)
                            MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.bodyMedium
                    )

                    Text(
                        text = description,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        }
    }
}

@Composable
private fun IndicatorsPanel(
    modifier: Modifier = Modifier
) {
    val indicators = listOf(
        "Moving Average" to listOf("SMA", "EMA", "WMA"),
        "Oscillators" to listOf("RSI", "MACD", "Stochastic", "CCI"),
        "Volatility" to listOf("Bollinger Bands", "ATR", "Keltner Channels"),
        "Volume" to listOf("Volume", "OBV", "MFI", "VWAP")
    )

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 8.dp)
    ) {
        indicators.forEach { (category, indicatorList) ->
            item {
                Text(
                    text = category,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.labelSmall
                )
            }

            items(indicatorList) { indicator ->
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { /* Добавить индикатор */ }
                ) {
                    Text(
                        text = indicator,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 8.dp),
                        color = MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

@Composable
private fun DrawingsPanel(
    modifier: Modifier = Modifier
) {
    val drawingTools = listOf(
        "Trend Line",
        "Horizontal Line",
        "Vertical Line",
        "Fibonacci Retracement",
        "Fibonacci Extension",
        "Rectangle",
        "Ellipse",
        "Triangle",
        "Channel",
        "Text"
    )

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 4.dp)
    ) {
        items(drawingTools) { tool ->
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { /* Выбрать инструмент */ }
            ) {
                Text(
                    text = tool,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
private fun StrategiesPanel(
    modifier: Modifier = Modifier
) {
    val strategies = listOf(
        "Moving Average Crossover",
        "RSI Overbought/Oversold",
        "MACD Divergence",
        "Bollinger Bounce",
        "Breakout Strategy",
        "Scalping Strategy",
        "Grid Trading"
    )

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 4.dp)
    ) {
        items(strategies) { strategy ->
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { /* Выбрать стратегию */ }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp)
                ) {
                    Text(
                        text = strategy,
                        color = MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.bodyMedium
                    )

                    // Можно добавить кнопку для применения стратегии
                    Text(
                        text = "Click to apply",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        }
    }
}

@Composable
private fun BottomToolPanel(
    type: BottomToolType,
    width: Dp,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.width(width),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Заголовок
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = when (type) {
                        BottomToolType.PORTFOLIO -> "Portfolio"
                        BottomToolType.CONSOLE -> "Console"
                        BottomToolType.EDITOR -> "Code Editor"
                    },
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )

                IconButton(
                    onClick = onClose,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        painter = painterResource(Res.drawable.close),
                        contentDescription = "Close",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Divider()

            // Контент
            when (type) {
                BottomToolType.PORTFOLIO -> PortfolioPanel(modifier = Modifier.weight(1f))
                BottomToolType.CONSOLE -> ConsolePanel(modifier = Modifier.weight(1f))
                BottomToolType.EDITOR -> CodeEditorPanel(modifier = Modifier.weight(1f))
            }
        }
    }
}

// Панель портфеля с табами
@Composable
private fun PortfolioPanel(
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf(PortfolioTab.POSITIONS) }

    // Мок данные
    val positions = remember {
        listOf(
            MockPosition("BTCUSDT", TradeSide.BUY, 0.5, 45000.0, 46500.0, 750.0, 1.67),
            MockPosition("ETHUSDT", TradeSide.BUY, 5.0, 3200.0, 3150.0, -250.0, -1.56),
            MockPosition("SOLUSDT", TradeSide.SELL, 20.0, 110.0, 108.5, 30.0, 1.36)
        )
    }

    val orders = remember {
        listOf(
            MockOrder(
                "1",
                "BTCUSDT",
                TradeSide.BUY,
                "LIMIT",
                44000.0,
                0.1,
                0.0,
                System.currentTimeMillis() - 3600000,
                "OPEN"
            ),
            MockOrder(
                "2",
                "ETHUSDT",
                TradeSide.SELL,
                "MARKET",
                3180.0,
                2.0,
                2.0,
                System.currentTimeMillis() - 1800000,
                "FILLED"
            ),
            MockOrder(
                "3",
                "SOLUSDT",
                TradeSide.BUY,
                "LIMIT",
                105.0,
                10.0,
                0.0,
                System.currentTimeMillis() - 300000,
                "OPEN"
            )
        )
    }

    val balances = remember {
        listOf(
            MockBalance("USDT", 15000.0, 5000.0, 20000.0, 20000.0),
            MockBalance("BTC", 0.5, 0.1, 0.6, 27900.0),
            MockBalance("ETH", 2.0, 1.0, 3.0, 9450.0),
            MockBalance("SOL", 50.0, 20.0, 70.0, 7595.0)
        )
    }

    Column(
        modifier = modifier.fillMaxSize()
    ) {
        // Табы
        ScrollableTabRow(
            selectedTabIndex = selectedTab.ordinal,
            modifier = Modifier.fillMaxWidth(),
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            edgePadding = 0.dp
        ) {
            PortfolioTab.values().forEach { tab ->
                Tab(
                    selected = selectedTab == tab,
                    onClick = { selectedTab = tab },
                    text = {
                        Text(
                            text = tab.name,
                            color = if (selectedTab == tab)
                                MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                )
            }
        }

        Divider()

        // Контент табов
        when (selectedTab) {
            PortfolioTab.POSITIONS -> PositionsList(positions)
            PortfolioTab.ORDERS -> OrdersList(orders)
            PortfolioTab.BALANCE -> BalanceList(balances)
            PortfolioTab.STATS -> TradingStats()
        }
    }
}

// Список позиций
@Composable
private fun PositionsList(
    positions: List<MockPosition>,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 4.dp)
    ) {
        // Заголовки
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Symbol", Modifier.weight(1f), style = MaterialTheme.typography.labelSmall)
                Text("Size", Modifier.weight(1f), style = MaterialTheme.typography.labelSmall)
                Text("Entry", Modifier.weight(1f), style = MaterialTheme.typography.labelSmall)
                Text("Mark", Modifier.weight(1f), style = MaterialTheme.typography.labelSmall)
                Text("PnL", Modifier.weight(1f), style = MaterialTheme.typography.labelSmall)
                Text("Action", Modifier.weight(0.5f), style = MaterialTheme.typography.labelSmall)
            }
        }

        items(positions) { position ->
            val pnlColor = if (position.pnl >= 0)
                MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.secondary

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = position.symbol,
                    Modifier.weight(1f),
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.bodySmall
                )

                Text(
                    text = String.format("%.3f", position.quantity),
                    Modifier.weight(1f),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall
                )

                Text(
                    text = String.format("%.1f", position.entryPrice),
                    Modifier.weight(1f),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall
                )

                Text(
                    text = String.format("%.1f", position.currentPrice),
                    Modifier.weight(1f),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall
                )

                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = String.format("%.2f", position.pnl),
                        color = pnlColor,
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = String.format("(%.2f%%)", position.pnlPercent),
                        color = pnlColor.copy(alpha = 0.7f),
                        style = MaterialTheme.typography.labelSmall
                    )
                }

                // Кнопка закрытия
                IconButton(
                    onClick = { /* Close position */ },
                    modifier = Modifier
                        .weight(0.5f)
                        .size(24.dp)
                ) {
                    Icon(
                        painter = painterResource(Res.drawable.close),
                        contentDescription = "Close",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Divider(modifier = Modifier.padding(horizontal = 8.dp))
        }
    }
}

// Список ордеров
@Composable
private fun OrdersList(
    orders: List<MockOrder>,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 4.dp)
    ) {
        // Заголовки
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Symbol", Modifier.weight(1f), style = MaterialTheme.typography.labelSmall)
                Text("Side/Type", Modifier.weight(1f), style = MaterialTheme.typography.labelSmall)
                Text("Price", Modifier.weight(1f), style = MaterialTheme.typography.labelSmall)
                Text("Qty/Filled", Modifier.weight(1f), style = MaterialTheme.typography.labelSmall)
                Text("Time", Modifier.weight(1f), style = MaterialTheme.typography.labelSmall)
                Text("Status", Modifier.weight(0.8f), style = MaterialTheme.typography.labelSmall)
                Text("", Modifier.weight(0.5f), style = MaterialTheme.typography.labelSmall)
            }
        }

        items(orders) { order ->
            val sideColor = if (order.side == TradeSide.BUY)
                MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.secondary

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = order.symbol,
                    Modifier.weight(1f),
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.bodySmall
                )

                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = order.side.name,
                        color = sideColor,
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = order.type,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.labelSmall
                    )
                }

                Text(
                    text = String.format("%.1f", order.price),
                    Modifier.weight(1f),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall
                )

                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = String.format("%.3f", order.quantity),
                        color = MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = "Filled: ${String.format("%.3f", order.filled)}",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.labelSmall
                    )
                }

                Text(
                    text = formatTime(order.timestamp),
                    Modifier.weight(1f),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall
                )

                Text(
                    text = order.status,
                    Modifier.weight(0.8f),
                    color = when (order.status) {
                        "OPEN" -> Color.Yellow
                        "FILLED" -> MaterialTheme.colorScheme.primary
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    style = MaterialTheme.typography.bodySmall
                )

                // Кнопка отмены (только для OPEN ордеров)
                if (order.status == "OPEN") {
                    IconButton(
                        onClick = { /* Cancel order */ },
                        modifier = Modifier
                            .weight(0.5f)
                            .size(24.dp)
                    ) {
                        Icon(
                            painter = painterResource(Res.drawable.close),
                            contentDescription = "Cancel",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.weight(0.5f))
                }
            }

            Divider(modifier = Modifier.padding(horizontal = 8.dp))
        }
    }
}

// Список балансов
@Composable
private fun BalanceList(
    balances: List<MockBalance>,
    modifier: Modifier = Modifier
) {
    // Общая стоимость портфеля
    val totalValue = balances.sumOf { it.usdValue }

    Column(
        modifier = modifier.fillMaxSize()
    ) {
        // Общая стоимость
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(12.dp)
            ) {
                Text(
                    text = "Total Portfolio Value",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelSmall
                )
                Text(
                    text = String.format("$%,.2f", totalValue),
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.headlineSmall
                )
            }
        }

        Divider()

        // Список активов
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(vertical = 4.dp)
        ) {
            // Заголовки
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Asset", Modifier.weight(1f), style = MaterialTheme.typography.labelSmall)
                    Text("Free", Modifier.weight(1f), style = MaterialTheme.typography.labelSmall)
                    Text("Locked", Modifier.weight(1f), style = MaterialTheme.typography.labelSmall)
                    Text("Total", Modifier.weight(1f), style = MaterialTheme.typography.labelSmall)
                    Text("USD Value", Modifier.weight(1f), style = MaterialTheme.typography.labelSmall)
                }
            }

            items(balances) { balance ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = balance.asset,
                        Modifier.weight(1f),
                        color = MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.bodySmall
                    )

                    Text(
                        text = String.format("%.3f", balance.free),
                        Modifier.weight(1f),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall
                    )

                    Text(
                        text = String.format("%.3f", balance.locked),
                        Modifier.weight(1f),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall
                    )

                    Text(
                        text = String.format("%.3f", balance.total),
                        Modifier.weight(1f),
                        color = MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.bodySmall
                    )

                    Text(
                        text = String.format("$%,.2f", balance.usdValue),
                        Modifier.weight(1f),
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Divider(modifier = Modifier.padding(horizontal = 8.dp))
            }
        }
    }
}

// Статистика торговли
@Composable
private fun TradingStats(
    modifier: Modifier = Modifier
) {
    // Мок статистика
    val stats = remember {
        mapOf(
            "Total Trades" to "156",
            "Winning Trades" to "89",
            "Losing Trades" to "67",
            "Win Rate" to "57.1%",
            "Total PnL" to "+$2,450.50",
            "Avg Win" to "$85.20",
            "Avg Loss" to "-$42.30",
            "Largest Win" to "$450.00",
            "Largest Loss" to "-$180.00",
            "Profit Factor" to "2.15",
            "Sharpe Ratio" to "1.82",
            "Max Drawdown" to "-12.5%"
        )
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(12.dp)
    ) {
        items(stats.toList()) { (key, value) ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = key,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall
                )

                val valueColor = when {
                    value.startsWith("+") -> MaterialTheme.colorScheme.primary
                    value.startsWith("-") -> MaterialTheme.colorScheme.secondary
                    else -> MaterialTheme.colorScheme.onSurface
                }

                Text(
                    text = value,
                    color = valueColor,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Divider(modifier = Modifier.padding(vertical = 4.dp))
        }
    }
}

// Консоль (исправленная версия)
@Composable
private fun ConsolePanel(
    modifier: Modifier = Modifier
) {
    val consoleLines = remember {
        mutableStateListOf(
            "[INFO] Terminal initialized",
            "[INFO] Connected to Binance",
            "[INFO] WebSocket connected for BTCUSDT",
            "[DEBUG] Order book snapshot received",
            "[TRADE] BUY 0.1 BTC @ 46,500.00",
            "[INFO] Position opened: BTCUSDT",
            "[WARN] High volatility detected",
            "[ERROR] Failed to place order: Insufficient balance",
            "[INFO] Reconnecting WebSocket...",
            "[INFO] WebSocket reconnected",
            "[TRADE] SELL 0.05 BTC @ 46,800.00",
            "[INFO] Position closed: Profit $150.00"
        )
    }

    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope() // 👈 Добавляем coroutine scope

    Column(
        modifier = modifier.fillMaxSize()
    ) {
        // Заголовок с кнопкой очистки
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "REPL Console",
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.labelMedium
            )

            IconButton(
                onClick = {
                    consoleLines.clear()
                    // Можно добавить системное сообщение
                    consoleLines.add("[INFO] Console cleared")
                },
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    painter = painterResource(Res.drawable.trash),
                    contentDescription = "Clear",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        Divider()

        // Лог консоли
        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
            reverseLayout = true // 👈 Переворачиваем список чтобы новые сообщения были снизу
        ) {
            items(consoleLines.size) { index ->
                val line = consoleLines[index] // Теперь индексы в правильном порядке
                val color = when {
                    line.startsWith("[ERROR]") -> MaterialTheme.colorScheme.secondary
                    line.startsWith("[WARN]") -> Color.Yellow
                    line.startsWith("[TRADE]") -> MaterialTheme.colorScheme.primary
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                }

                Text(
                    text = line,
                    color = color,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp
                    ),
                    modifier = Modifier.padding(vertical = 2.dp)
                )
            }
        }

        Divider()

        // Поле ввода
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = ">",
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(end = 4.dp)
            )

            BasicTextField(
                value = inputText,
                onValueChange = { inputText = it },
                modifier = Modifier
                    .weight(1f)
                    .height(32.dp),
                textStyle = TextStyle(
                    color = MaterialTheme.colorScheme.onSurface,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp
                ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                decorationBox = { innerTextField ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                shape = MaterialTheme.shapes.small
                            )
                            .padding(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        if (inputText.isEmpty()) {
                            Text(
                                text = "Type command...",
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        innerTextField()
                    }
                }
            )

            IconButton(
                onClick = {
                    if (inputText.isNotBlank()) {
                        // Добавляем команду в лог
                        consoleLines.add(0, "[INPUT] $inputText") // Добавляем в начало (сверху)

                        // Обработка команд
                        processCommand(inputText, consoleLines)

                        // Очищаем поле ввода
                        inputText = ""

                        // Скроллим к началу (к новым сообщениям)
                        coroutineScope.launch {
                            listState.animateScrollToItem(0)
                        }
                    }
                },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    painter = painterResource(Res.drawable.send),
                    contentDescription = "Send",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

// Функция для обработки команд
private fun processCommand(command: String, consoleLines: MutableList<String>) {
    val trimmed = command.trim().lowercase()

    when {
        trimmed == "clear" || trimmed == "cls" -> {
            consoleLines.clear()
            consoleLines.add("[INFO] Console cleared")
        }

        trimmed == "help" -> {
            consoleLines.add(0, "[INFO] Available commands:")
            consoleLines.add(0, "[INFO]   help     - Show this help")
            consoleLines.add(0, "[INFO]   clear    - Clear console")
            consoleLines.add(0, "[INFO]   status   - Show system status")
            consoleLines.add(0, "[INFO]   symbols  - List available symbols")
            consoleLines.add(0, "[INFO]   time     - Show current time")
        }

        trimmed == "status" -> {
            consoleLines.add(0, "[INFO] System Status:")
            consoleLines.add(0, "[INFO]   Connected to Binance: ✅")
            consoleLines.add(0, "[INFO]   WebSocket: ✅")
            consoleLines.add(0, "[INFO]   Memory usage: 256MB")
            consoleLines.add(0, "[INFO]   Uptime: 2h 15m")
        }

        trimmed == "symbols" -> {
            consoleLines.add(0, "[INFO] Available symbols:")
            consoleLines.add(0, "[INFO]   BTCUSDT, ETHUSDT, BNBUSDT, SOLUSDT, XRPUSDT")
            consoleLines.add(0, "[INFO]   ADAUSDT, DOGEUSDT, DOTUSDT, LINKUSDT, LTCUSDT")
        }

        trimmed.startsWith("echo ") -> {
            val message = command.substring(5)
            consoleLines.add(0, "[ECHO] $message")
        }

        trimmed == "time" -> {
            val time = SimpleDateFormat("HH:mm:ss").format(Date())
            consoleLines.add(0, "[INFO] Current time: $time")
        }

        command.isNotBlank() -> {
            consoleLines.add(0, "[ERROR] Unknown command: $command")
            consoleLines.add(0, "[INFO] Type 'help' for available commands")
        }
    }
}

// Редактор кода
@Composable
private fun CodeEditorPanel(
    modifier: Modifier = Modifier
) {
    var selectedFile by remember { mutableStateOf("Moving Average Strategy") }
    var code by remember {
        mutableStateOf(
            """// Moving Average Crossover Strategy
indicator("MA Crossover", overlay=true)

fastLength = input(9, "Fast MA")
slowLength = input(21, "Slow MA")

fastMA = ta.sma(close, fastLength)
slowMA = ta.sma(close, slowLength)

plot(fastMA, color=color.blue)
plot(slowMA, color=color.red)

// Entry signals
if (ta.crossover(fastMA, slowMA))
    strategy.entry("Long", strategy.long)

if (ta.crossunder(fastMA, slowMA))
    strategy.entry("Short", strategy.short)"""
        )
    }

    val files = listOf(
        "Moving Average Strategy",
        "RSI Strategy",
        "MACD Divergence",
        "Grid Bot",
        "Scalping Strategy"
    )

    Column(
        modifier = modifier.fillMaxSize()
    ) {
        // Верхняя панель с файлами
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                files.forEach { file ->
                    FilterChip(
                        selected = selectedFile == file,
                        onClick = { selectedFile = file },
                        label = {
                            Text(
                                text = file,
                                style = MaterialTheme.typography.labelSmall
                            )
                        },
                        modifier = Modifier.height(32.dp)
                    )
                }

                IconButton(
                    onClick = { /* New file */ },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        painter = painterResource(Res.drawable.icon_plus),
                        contentDescription = "New",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        Divider()

        // Редактор кода
        Box(
            modifier = Modifier
                .weight(1f)
                .background(Color(0xFF1E1E1E)) // Темный фон как в IDE
                .padding(8.dp)
        ) {
            BasicTextField(
                value = code,
                onValueChange = { code = it },
                modifier = Modifier.fillMaxSize(),
                textStyle = TextStyle(
                    color = Color(0xFFD4D4D4),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                decorationBox = { innerTextField ->
                    Box(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        innerTextField()
                    }
                }
            )
        }

        Divider()

        // Нижняя панель с кнопками
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TerminalButton(
                onClick = { /* Save */ },
                modifier = Modifier.weight(1f)
            ) {
                Text("Save")
            }

            TerminalButton(
                onClick = { /* Compile */ },
                modifier = Modifier.weight(1f)
            ) {
                Text("Compile")
            }

            TerminalButton(
                onClick = { /* Run */ },
                modifier = Modifier.weight(1f),
                isActive = true
            ) {
                Text("Run")
            }
        }
    }
}