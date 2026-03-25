package com.aandios.nous.feature.dom.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.aandios.nous.feature.dom.domain.*

@Composable
fun DomHeader(
    tradingProvider: TradingProvider,
    onTradingProviderChanged: (TradingProvider) -> Unit,
    tradingSymbol: TradingSymbol,
    onSymbolChanged: (TradingSymbol) -> Unit,
    depthLimit: DepthLimit,
    onDepthLimitChanged: (DepthLimit) -> Unit,
    aggregationLevel: AggregationLevel,
    onAggregationLevelChanged: (AggregationLevel) -> Unit,
    domMode: DomMode,
    onDomModeChanged: (DomMode) -> Unit,
    isLive: Boolean = true,
    collapsed: Boolean = false,
    onToggleCollapsed: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (collapsed) {
        // Компактный режим: только provider и symbol
        CompactDomHeader(
            tradingProvider = tradingProvider,
            tradingSymbol = tradingSymbol,
            isLive = isLive,
            isExpanded = false,
            onToggleExpand = onToggleCollapsed,
            modifier = modifier
        )
    } else {
        // Полный режим: все dropdown с labels
        ExpandedDomHeader(
            tradingProvider = tradingProvider,
            onTradingProviderChanged = onTradingProviderChanged,
            tradingSymbol = tradingSymbol,
            onSymbolChanged = onSymbolChanged,
            depthLimit = depthLimit,
            onDepthLimitChanged = onDepthLimitChanged,
            aggregationLevel = aggregationLevel,
            onAggregationLevelChanged = onAggregationLevelChanged,
            domMode = domMode,
            onDomModeChanged = onDomModeChanged,
            isLive = isLive,
            onToggleCollapsed = onToggleCollapsed,
            modifier = modifier
        )
    }
}

/**
 * Развернутая версия DomHeader со всеми dropdown и кнопкой сворачивания.
 */
@Composable
private fun ExpandedDomHeader(
    tradingProvider: TradingProvider,
    onTradingProviderChanged: (TradingProvider) -> Unit,
    tradingSymbol: TradingSymbol,
    onSymbolChanged: (TradingSymbol) -> Unit,
    depthLimit: DepthLimit,
    onDepthLimitChanged: (DepthLimit) -> Unit,
    aggregationLevel: AggregationLevel,
    onAggregationLevelChanged: (AggregationLevel) -> Unit,
    domMode: DomMode,
    onDomModeChanged: (DomMode) -> Unit,
    isLive: Boolean = true,
    onToggleCollapsed: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 1.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Первая строка: provider + live индикатор + кнопка сворачивания
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Provider dropdown с label
                TradingProviderDropdownWithLabel(
                    currentProvider = tradingProvider,
                    onProviderChanged = onTradingProviderChanged,
                    modifier = Modifier.weight(1f)
                )
                
                // Правая часть: live индикатор + кнопка сворачивания
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Live индикатор
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(
                                    color = if (isLive) Color.Green else Color.Red,
                                    shape = MaterialTheme.shapes.small
                                )
                        )
                        Text(
                            text = if (isLive) "LIVE" else "OFFLINE",
                            color = if (isLive) Color.Green else Color.Red,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Medium
                        )
                    }
                    
                    // Кнопка сворачивания
                    IconButton(
                        onClick = onToggleCollapsed,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = "Свернуть",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.rotate(180f)
                        )
                    }
                }
            }

            // Вторая строка: symbol dropdown + depth limit selector
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Symbol dropdown с label
                SymbolDropdownWithLabel(
                    currentSymbol = tradingSymbol,
                    provider = tradingProvider,
                    onSymbolChanged = onSymbolChanged,
                    modifier = Modifier.weight(1.5f)
                )
                
                // Depth limit selector с label
                DepthLimitSelectorWithLabel(
                    currentLimit = depthLimit,
                    onLimitChanged = onDepthLimitChanged,
                    modifier = Modifier.weight(1f)
                )
            }

            // Третья строка: aggregation level + DOM mode
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Aggregation level dropdown с label
                AggregationLevelDropdownWithLabel(
                    currentLevel = aggregationLevel,
                    onLevelChanged = onAggregationLevelChanged,
                    modifier = Modifier.weight(1f)
                )
                
                // DOM mode dropdown с label
                DomModeDropdownWithLabel(
                    currentMode = domMode,
                    onModeChanged = onDomModeChanged,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}