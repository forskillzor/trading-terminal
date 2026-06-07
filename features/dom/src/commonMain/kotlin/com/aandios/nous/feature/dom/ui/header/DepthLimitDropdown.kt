package com.aandios.nous.feature.dom.ui.header

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.aandios.nous.core.ui.component.TerminalDropdownWithLabel
import com.aandios.nous.feature.dom.domain.model.DepthLimit
import com.aandios.nous.feature.dom.ui.DepthLimitSelector

/**
 * Вспомогательная функция для создания dropdown с label для Depth Limit.
 */
@Composable
fun DepthLimitDropdown(
    currentLimit: DepthLimit,
    onLimitChanged: (DepthLimit) -> Unit,
    modifier: Modifier = Modifier.Companion
) {
    TerminalDropdownWithLabel(
        label = "Depth",
        modifier = modifier
    ) {
        DepthLimitSelector(
            currentLimit = currentLimit,
            onLimitChanged = onLimitChanged
        )
    }
}