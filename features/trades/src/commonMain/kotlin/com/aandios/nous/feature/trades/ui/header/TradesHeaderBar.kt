package com.aandios.nous.feature.trades.ui.header

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.aandios.nous.api.market.model.SymbolInfo
import com.aandios.nous.feature.trades.ui.SizeFilter

/**
 * Верхняя панель с symbol dropdown и size filter dropdown (как в DomHeader).
 */
@Composable
fun TradesHeaderBar(
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
        modifier = Modifier.Companion.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.Companion
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.Companion.CenterVertically
        ) {
            // Symbol dropdown
            TradesSymbolDropdown(
                currentSymbol = currentSymbol,
                availableSymbols = availableSymbols,
                onSymbolChanged = onSymbolChanged,
                modifier = Modifier.Companion.weight(1.4f)
            )

            // Size filter dropdown
            SizeFilterDropdown(
                currentFilter = selectedSizeFilter,
                minQty = currentSymbolInfo?.minQty,
                onFilterChanged = onSizeFilterChanged,
                modifier = Modifier.Companion.weight(1f)
            )

            // Live индикатор
            Box(
                modifier = Modifier.Companion
                    .size(8.dp)
                    .background(
                        color = Color.Companion.Green,
                        shape = MaterialTheme.shapes.small
                    )
            )
        }
    }
}