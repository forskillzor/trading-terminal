package com.aandios.nous.feature.dom.ui.header

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aandios.nous.core.ui.component.TerminalDropdown
import com.aandios.nous.core.ui.component.TerminalDropdownWithLabel
import com.aandios.nous.feature.dom.domain.DomMode

/**
 * Вспомогательная функция для создания dropdown с label для DOM Mode.
 */
@Composable
fun DomModeDropdown(
    currentMode: DomMode,
    onModeChanged: (DomMode) -> Unit,
    modifier: Modifier = Modifier.Companion
) {
    TerminalDropdownWithLabel(
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