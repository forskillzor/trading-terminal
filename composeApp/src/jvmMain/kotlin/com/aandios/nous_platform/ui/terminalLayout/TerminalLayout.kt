package com.aandios.nous_platform.ui.terminalLayout

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.Divider
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.aandios.nous.api.market.model.trading.TradeSide
import nous_platform.composeapp.generated.resources.*

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
fun TerminalLayout(
    modifier: Modifier = Modifier,
    terminalState: TerminalStateViewModel,
    onSymbolSelected: (String) -> Unit,
    onTimeframeSelected: (String) -> Unit,
    mainContent: @Composable ColumnScope.() -> Unit
) {
    var topPanelState by remember { mutableStateOf(ToolPanelState()) }
    var bottomPanelState by remember { mutableStateOf<BottomToolType?>(null) }
    val selectedSymbol by remember { mutableStateOf<String>(terminalState.selectedSymbol.value) }
    val selectedTimeframe by remember { mutableStateOf<String>(terminalState.selectedTimeFrame.value) }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {

        // Используем Row для размещения основной панели и выдвижных панелей рядом
        Row(
//            modifier = modifier.fillMaxHeight()
        ) {
            // Основная панель с иконками (всегда видима)
            Column(
                modifier = Modifier
//                    .fillMaxHeight()
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
                )
            }
            Column() {
                mainContent()
                if (bottomPanelState != null) {

                    BottomToolPanel(
                        type = bottomPanelState!!,
                        width = Dp.Unspecified,
                        onClose = { bottomPanelState = null },
                        modifier = Modifier.fillMaxWidth()
                            .height(300.dp)
                            .weight(0.5f)
                    )
                }
            }
        }
    }
}

