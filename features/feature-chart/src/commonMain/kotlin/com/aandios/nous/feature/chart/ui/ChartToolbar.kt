package com.aandios.nous.feature.chart.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aandios.nous.core.ui.component.TerminalDropdownWithLabel

private val timeframes = listOf("1m", "5m", "15m", "30m", "1h", "4h", "1d", "1w")

private val toolbarBg = Color.Black.copy(alpha = 0.35f)
private val accentColor = Color(0xFF5B9BD5) // мягкий голубой для активного

@Composable
fun ChartToolbar(
    currentSymbol: String,
    currentTimeframe: String,
    availableSymbols: List<String>,
    onSymbolChange: (String) -> Unit,
    onTimeframeChange: (String) -> Unit,
    crosshairEnabled: Boolean = false,
    onCrosshairToggle: () -> Unit = {},
    chartMode: ChartMode = ChartMode.CANDLESTICK,
    onChartModeToggle: () -> Unit = {},
    symbolsWithFootprint: Set<String> = emptySet(),
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .background(toolbarBg, RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SymbolSelector(
            currentSymbol = currentSymbol,
            availableSymbols = availableSymbols,
            onSymbolChange = onSymbolChange,
            symbolsWithFootprint = symbolsWithFootprint,
        )

        Spacer(Modifier.width(12.dp))

        ChartModeToggleButton(
            mode = chartMode,
            onToggle = onChartModeToggle,
        )

        Spacer(Modifier.width(8.dp))

        CrosshairToggleButton(
            enabled = crosshairEnabled,
            onToggle = onCrosshairToggle,
        )

        Spacer(Modifier.width(8.dp))

        TimeframeSelector(
            currentTimeframe = currentTimeframe,
            onTimeframeChange = onTimeframeChange,
        )
    }
}

@Composable
private fun ChartModeToggleButton(
    mode: ChartMode,
    onToggle: () -> Unit,
) {
    val label = when (mode) {
        ChartMode.CANDLESTICK -> "C"
        ChartMode.FOOTPRINT -> "FP"
    }
    val activeColor = accentColor

    Text(
        text = label,
        color = if (mode == ChartMode.FOOTPRINT) activeColor else MaterialTheme.colorScheme.surfaceVariant,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        fontFamily = FontFamily.Monospace,
        modifier = Modifier
            .clickable { onToggle() }
            .background(
                if (mode == ChartMode.FOOTPRINT) accentColor.copy(alpha = 0.25f)
                else Color.Transparent,
                RoundedCornerShape(3.dp),
            )
            .padding(horizontal = 6.dp, vertical = 3.dp),
    )
}

@Composable
private fun CrosshairToggleButton(
    enabled: Boolean,
    onToggle: () -> Unit,
) {
    Text(
        text = "\u29C9",  // ⧉ — символ перекрестия
        color = if (enabled) MaterialTheme.colorScheme.inverseOnSurface else MaterialTheme.colorScheme.surfaceVariant,
        fontSize = 14.sp,
        fontWeight = FontWeight.Bold,
        fontFamily = FontFamily.Monospace,
        modifier = Modifier
            .clickable { onToggle() }
            .background(
                if (enabled) accentColor.copy(alpha = 0.25f)
                else Color.Transparent,
                RoundedCornerShape(3.dp),
            )
            .padding(horizontal = 6.dp, vertical = 3.dp),
    )
}

@Composable
private fun SymbolSelector(
    currentSymbol: String,
    availableSymbols: List<String>,
    onSymbolChange: (String) -> Unit,
    symbolsWithFootprint: Set<String> = emptySet(),
) {
    var expanded by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    // Сбрасываем поиск при открытии/закрытии
    if (!expanded) {
        searchQuery = ""
    }

    val filteredSymbols = remember(availableSymbols, searchQuery) {
        if (searchQuery.isBlank()) availableSymbols
        else availableSymbols.filter { it.contains(searchQuery, ignoreCase = true) }
    }

    Box {
        // Триггер в стиле TerminalDropdownWithLabel (рамка + label Sym + стрелка)
        TerminalDropdownWithLabel(label = "Sym") {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable { expanded = true }
            ) {
                Text(
                    text = currentSymbol,
                    color = MaterialTheme.colorScheme.inverseOnSurface,
                    fontSize = MaterialTheme.typography.labelSmall.fontSize,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 0.2.sp,
                    maxLines = 1,
                )
                Spacer(Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = "Select symbol",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            offset = DpOffset(0.dp, 4.dp),
            modifier = Modifier
                .widthIn(min = 180.dp, max = 280.dp)
                .heightIn(max = 320.dp)
                .background(Color(0xFF1E1E1E)),
        ) {
            // Поле поиска
            BasicTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                singleLine = true,
                textStyle = TextStyle(
                    color = Color.White,
                    fontSize = 13.sp,
                    fontFamily = FontFamily.Monospace,
                ),
                cursorBrush = SolidColor(Color.White),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp)
                    .background(Color.White.copy(alpha = 0.08f), RoundedCornerShape(4.dp))
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                decorationBox = { innerTextField ->
                    if (searchQuery.isEmpty()) {
                        Text(
                            text = "Search symbol...",
                            color = Color.White.copy(alpha = 0.3f),
                            fontSize = 13.sp,
                            fontFamily = FontFamily.Monospace,
                        )
                    }
                    innerTextField()
                },
            )

            // Разделитель
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .padding(horizontal = 12.dp)
                    .background(Color.White.copy(alpha = 0.1f))
            )

            // Список символов
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 260.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                filteredSymbols.forEach { symbol ->
                    DropdownMenuItem(
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = symbol,
                                    color = if (symbol == currentSymbol)
                                        accentColor else Color.White.copy(alpha = 0.85f),
                                    fontSize = 12.sp,
                                    fontFamily = FontFamily.Monospace,
                                )
                                if (symbol in symbolsWithFootprint) {
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        text = "md",
                                        color = accentColor.copy(alpha = 0.7f),
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace,
                                    )
                                }
                            }
                        },
                        onClick = {
                            onSymbolChange(symbol)
                            expanded = false
                            searchQuery = ""
                        },
                        modifier = Modifier
                            .background(
                                if (symbol == currentSymbol)
                                    accentColor.copy(alpha = 0.12f)
                                else Color.Transparent
                            ),
                    )
                }

                if (filteredSymbols.isEmpty()) {
                    Text(
                        text = "No symbols found",
                        color = Color.White.copy(alpha = 0.4f),
                        fontSize = 12.sp,
                        modifier = Modifier.padding(12.dp),
                    )
                }
            }
        }
    }
}
@Composable
private fun TimeframeSelector(
    currentTimeframe: String,
    onTimeframeChange: (String) -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        timeframes.forEach { tf ->
            val isActive = tf == currentTimeframe

            Text(
                text = tf,
                color = if (isActive) MaterialTheme.colorScheme.inverseOnSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp,
                fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier
                    .clickable { onTimeframeChange(tf) }
                    .background(
                        if (isActive) accentColor.copy(alpha = 0.25f)
                        else Color.Transparent,
                        RoundedCornerShape(3.dp),
                    )
                    .padding(horizontal = 5.dp, vertical = 3.dp),
            )

            if (tf != timeframes.last()) {
                Spacer(Modifier.width(2.dp))
            }
        }
    }
}

