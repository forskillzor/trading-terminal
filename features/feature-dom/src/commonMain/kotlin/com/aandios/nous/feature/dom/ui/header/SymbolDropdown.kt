package com.aandios.nous.feature.dom.ui.header

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aandios.nous.core.ui.component.TerminalDropdown
import com.aandios.nous.core.ui.component.TerminalDropdownWithLabel
import com.aandios.nous.feature.dom.domain.TradingProvider
import com.aandios.nous.feature.dom.domain.TradingSymbol

/**
 * Вспомогательная функция для создания dropdown с label для Symbol.
 */
@Composable
fun SymbolDropdown(
    currentSymbol: TradingSymbol,
    provider: TradingProvider,
    onSymbolChanged: (TradingSymbol) -> Unit,
    modifier: Modifier = Modifier.Companion
) {
    TerminalDropdownWithLabel(
        label = "Sym",
        modifier = modifier
    ) {
        TerminalDropdown(
            currentValue = currentSymbol,
            items = TradingSymbol.Companion.getSymbolsForProvider(provider),
            onValueChanged = onSymbolChanged,
            displayText = { it.displayName },
            menuWidth = 120.dp
        )
    }
}