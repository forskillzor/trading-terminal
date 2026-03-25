package com.aandios.nous.feature.dom.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aandios.nous.feature.dom.domain.*

/**
 * Обертка для dropdown-компонента с label слева и бордером.
 * 
 * @param label Текст label, отображаемый слева от dropdown
 * @param content Composable-функция, содержащая сам dropdown
 * @param modifier Modifier для контейнера
 */
@Composable
fun DropdownWithLabel(
    label: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Surface(
        shape = MaterialTheme.shapes.extraSmall,
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.8f)
        ),
        color = Color.Transparent,
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 0.dp)
        ) {
            // Label слева
            Text(
                text = label,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = MaterialTheme.typography.labelSmall.fontSize,
                fontWeight = FontWeight.Medium,
                letterSpacing = 0.1.sp,
                modifier = Modifier.padding(end = 8.dp)
            )
            
            // Вертикальный разделитель
            Spacer(
                modifier = Modifier
                    .width(2.dp)
                    .height(18.dp)
                    .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.8f))
            )
            
            // Dropdown справа
            Box(
                modifier = Modifier.padding(start = 8.dp)
            ) {
                content()
            }
        }
    }
}

/**
 * Вспомогательная функция для создания dropdown с label для TradingProvider.
 */
@Composable
fun TradingProviderDropdownWithLabel(
    currentProvider: TradingProvider,
    onProviderChanged: (TradingProvider) -> Unit,
    modifier: Modifier = Modifier
) {
    DropdownWithLabel(
        label = "Provider",
        modifier = modifier
    ) {
        TerminalDropdown(
            currentValue = currentProvider,
            items = TradingProvider.all(),
            onValueChanged = onProviderChanged,
            displayText = { it.displayName },
            menuWidth = 180.dp
        )
    }
}

/**
 * Вспомогательная функция для создания dropdown с label для Symbol.
 */
@Composable
fun SymbolDropdownWithLabel(
    currentSymbol: TradingSymbol,
    provider: TradingProvider,
    onSymbolChanged: (TradingSymbol) -> Unit,
    modifier: Modifier = Modifier
) {
    DropdownWithLabel(
        label = "Sym",
        modifier = modifier
    ) {
        TerminalDropdown(
            currentValue = currentSymbol,
            items = TradingSymbol.getSymbolsForProvider(provider),
            onValueChanged = onSymbolChanged,
            displayText = { it.displayName },
            menuWidth = 120.dp
        )
    }
}

/**
 * Вспомогательная функция для создания dropdown с label для Depth Limit.
 */
@Composable
fun DepthLimitSelectorWithLabel(
    currentLimit: DepthLimit,
    onLimitChanged: (DepthLimit) -> Unit,
    modifier: Modifier = Modifier
) {
    DropdownWithLabel(
        label = "Depth",
        modifier = modifier
    ) {
        DepthLimitSelector(
            currentLimit = currentLimit,
            onLimitChanged = onLimitChanged
        )
    }
}

/**
 * Вспомогательная функция для создания dropdown с label для Aggregation Level.
 */
@Composable
fun AggregationLevelDropdownWithLabel(
    currentLevel: AggregationLevel,
    onLevelChanged: (AggregationLevel) -> Unit,
    modifier: Modifier = Modifier
) {
    DropdownWithLabel(
        label = "Agg",
        modifier = modifier
    ) {
        TerminalDropdown(
            currentValue = currentLevel,
            items = AggregationLevel.all(),
            onValueChanged = onLevelChanged,
            displayText = { it.displayName() },
            menuWidth = 100.dp
        )
    }
}

/**
 * Вспомогательная функция для создания dropdown с label для DOM Mode.
 */
@Composable
fun DomModeDropdownWithLabel(
    currentMode: DomMode,
    onModeChanged: (DomMode) -> Unit,
    modifier: Modifier = Modifier
) {
    DropdownWithLabel(
        label = "Mode",
        modifier = modifier
    ) {
        TerminalDropdown(
            currentValue = currentMode,
            items = DomMode.entries,
            onValueChanged = onModeChanged,
            displayText = { it.displayName },
            menuWidth = 100.dp
        )
    }
}