package com.aandios.nous.feature.dom.ui.header

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aandios.nous.feature.dom.domain.SplitViewMode

@Composable
fun SplitViewModeSelector(
    currentMode: SplitViewMode,
    onModeChanged: (SplitViewMode) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        SplitViewModeButton(
            mode = SplitViewMode.BID_ASK,
            currentMode = currentMode,
            onClick = { onModeChanged(SplitViewMode.BID_ASK) },
            text = "Both"
        )
        SplitViewModeButton(
            mode = SplitViewMode.BID_ONLY,
            currentMode = currentMode,
            onClick = { onModeChanged(SplitViewMode.BID_ONLY) },
            text = "Bid"
        )
        SplitViewModeButton(
            mode = SplitViewMode.ASK_ONLY,
            currentMode = currentMode,
            onClick = { onModeChanged(SplitViewMode.ASK_ONLY) },
            text = "Ask"
        )
    }
}

@Composable
private fun SplitViewModeButton(
    mode: SplitViewMode,
    currentMode: SplitViewMode,
    onClick: () -> Unit,
    text: String,
    modifier: Modifier = Modifier
) {
    val isSelected = mode == currentMode
    Box(
        modifier = modifier
            .width(60.dp)
            .background(
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                shape = MaterialTheme.shapes.small
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.labelSmall
        )
    }
}