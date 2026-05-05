package com.aandios.nous.feature.trades.ui.header

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aandios.nous.core.ui.component.TerminalDropdownWithLabel
import com.aandios.nous.feature.trades.ui.SizeFilter

/**
 * Dropdown для фильтрации сделок по минимальному объёму.
 * Опции генерируются на основе minQty из SymbolInfo.
 * Если minQty == null — показывается только "All".
 */
@Composable
fun SizeFilterDropdown(
    currentFilter: SizeFilter,
    minQty: Double?,
    onFilterChanged: (SizeFilter) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }

    val options = remember(minQty) {
        if (minQty != null) {
            listOf(
                SizeFilter.All,
                SizeFilter.MinQty,
                SizeFilter.MinQtyx10,
                SizeFilter.MinQtyx100,
            )
        } else {
            listOf(SizeFilter.All)
        }
    }

    TerminalDropdownWithLabel(
        label = "Size",
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
                ) {
                    Text(
                        text = currentFilter.label,
                        color = MaterialTheme.colorScheme.inverseOnSurface,
                        fontSize = MaterialTheme.typography.labelSmall.fontSize,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 0.2.sp,
                        maxLines = 1,
                    )
                    Spacer(Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = "Select size filter",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // Выпадающее меню
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.width(180.dp)
            ) {
                options.forEach { filter ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = formatFilterLabel(filter, minQty),
                                color = if (filter == currentFilter)
                                    MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                                fontSize = 12.sp,
                            )
                        },
                        onClick = {
                            onFilterChanged(filter)
                            expanded = false
                        },
                    )
                }
            }
        }
    }
}

/**
 * Форматирует лейбл фильтра с учётом minQty.
 */
private fun formatFilterLabel(filter: SizeFilter, minQty: Double?): String {
    if (minQty == null) return filter.label
    return when (filter) {
        SizeFilter.All -> filter.label
        SizeFilter.MinQty -> "≥ ${formatQty(minQty)}"
        SizeFilter.MinQtyx10 -> "≥ ${formatQty(minQty * 10)}"
        SizeFilter.MinQtyx100 -> "≥ ${formatQty(minQty * 100)}"
    }
}

private fun formatQty(value: Double): String {
    return when {
        value >= 1000 -> String.format("%.1f", value)
        value >= 1 -> String.format("%.3f", value)
        value >= 0.001 -> String.format("%.4f", value)
        else -> String.format("%.6f", value)
    }
}
