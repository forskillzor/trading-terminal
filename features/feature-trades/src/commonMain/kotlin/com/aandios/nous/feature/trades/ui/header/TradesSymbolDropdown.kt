package com.aandios.nous.feature.trades.ui.header

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aandios.nous.api.market.model.SymbolInfo
import com.aandios.nous.core.ui.component.TerminalDropdownWithLabel

/**
 * Dropdown выбора символа с автокомплитом для Trades.
 * Аналог SymbolDropdown из feature-dom, но работает напрямую с SymbolInfo
 * (не зависит от TradingSymbol и feature-dom).
 */
@Composable
fun TradesSymbolDropdown(
    currentSymbol: String,
    availableSymbols: List<SymbolInfo> = emptyList(),
    onSymbolChanged: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    // Сбрасываем поиск при закрытии
    if (!expanded) {
        searchQuery = ""
    }

    val symbols = remember(availableSymbols) {
        if (availableSymbols.isNotEmpty()) availableSymbols
        else fallbackSymbols
    }

    val filteredSymbols = remember(symbols, searchQuery) {
        if (searchQuery.isBlank()) symbols
        else symbols.filter {
            it.symbol.contains(searchQuery, ignoreCase = true) ||
            it.baseAsset.contains(searchQuery, ignoreCase = true) ||
            it.quoteAsset.contains(searchQuery, ignoreCase = true)
        }
    }

    TerminalDropdownWithLabel(
        label = "Sym",
        modifier = modifier
    ) {
        Box {
            // Триггер-кнопка
            Surface(
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.surfaceVariant,
                tonalElevation = 2.dp,
                modifier = Modifier.clickable { expanded = true }
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = formatSymbolDisplay(currentSymbol),
                        color = MaterialTheme.colorScheme.inverseOnSurface,
                        fontSize = MaterialTheme.typography.labelSmall.fontSize,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 0.2.sp,
                        maxLines = 1,
                    )
                    Spacer(Modifier.weight(1f))
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = "Select symbol",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // Выпадающее меню с автокомплитом
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.width(220.dp)
            ) {
                // Поле поиска
                BasicTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    singleLine = true,
                    textStyle = TextStyle(
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 13.sp,
                        fontFamily = FontFamily.Monospace,
                    ),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.onSurface),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            RoundedCornerShape(4.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    decorationBox = { innerTextField ->
                        if (searchQuery.isEmpty()) {
                            Text(
                                text = "Search symbol...",
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
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
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                )

                // Список символов
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 260.dp)
                        .verticalScroll(rememberScrollState()),
                ) {
                    filteredSymbols.forEach { symbolInfo ->
                        val displayName = formatSymbolDisplay(symbolInfo.symbol)
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = displayName,
                                    color = if (symbolInfo.symbol == currentSymbol)
                                        MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                                    fontSize = 12.sp,
                                    fontFamily = FontFamily.Monospace,
                                )
                            },
                            onClick = {
                                onSymbolChanged(symbolInfo.symbol)
                                expanded = false
                                searchQuery = ""
                            },
                            modifier = Modifier
                                .background(
                                    if (symbolInfo.symbol == currentSymbol)
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                                    else Color.Transparent
                                ),
                        )
                    }

                    if (filteredSymbols.isEmpty()) {
                        Text(
                            text = "No symbols found",
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                            fontSize = 12.sp,
                            modifier = Modifier.padding(12.dp),
                        )
                    }
                }
            }
        }
    }
}

/**
 * Форматирует символ для отображения: "BTCUSDT" -> "BTC/USDT"
 */
private fun formatSymbolDisplay(symbol: String): String {
    // Простое форматирование: найти, где заканчивается baseAsset
    // для популярных пар вида XXXUSDT, XXXBTC и т.д.
    val quoteAssets = listOf("USDT", "USDC", "BUSD", "BTC", "ETH", "BNB", "TRY", "EUR")
    for (quote in quoteAssets) {
        if (symbol.endsWith(quote) && symbol.length > quote.length) {
            val base = symbol.removeSuffix(quote)
            return "$base/$quote"
        }
    }
    return symbol
}

/**
 * Статический fallback-список символов, если SymbolInfo не загрузился.
 */
private val fallbackSymbols = listOf(
    SymbolInfo("BTCUSDT", 0.01, 0.001, 0.001, 10.0, "TRADING", "BTC", "USDT"),
    SymbolInfo("ETHUSDT", 0.01, 0.001, 0.001, 10.0, "TRADING", "ETH", "USDT"),
    SymbolInfo("BNBUSDT", 0.01, 0.001, 0.001, 10.0, "TRADING", "BNB", "USDT"),
    SymbolInfo("SOLUSDT", 0.01, 0.001, 0.001, 10.0, "TRADING", "SOL", "USDT"),
    SymbolInfo("XRPUSDT", 0.0001, 0.001, 0.001, 10.0, "TRADING", "XRP", "USDT"),
    SymbolInfo("ADAUSDT", 0.00001, 0.001, 0.001, 10.0, "TRADING", "ADA", "USDT"),
    SymbolInfo("DOGEUSDT", 0.00001, 0.001, 0.001, 10.0, "TRADING", "DOGE", "USDT"),
    SymbolInfo("DOTUSDT", 0.001, 0.001, 0.001, 10.0, "TRADING", "DOT", "USDT"),
    SymbolInfo("AVAXUSDT", 0.001, 0.001, 0.001, 10.0, "TRADING", "AVAX", "USDT"),
    SymbolInfo("LINKUSDT", 0.001, 0.001, 0.001, 10.0, "TRADING", "LINK", "USDT"),
)
