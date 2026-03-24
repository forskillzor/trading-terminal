package com.aandios.nous.feature.dom.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aandios.nous.feature.dom.domain.DomMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DomStyleToggle(
    currentMode: DomMode,
    onModeChanged: (DomMode) -> Unit,
    modifier: Modifier = Modifier
) {
    val modes = listOf(DomMode.CLASSIC, DomMode.NINJA)
    
    Surface(
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 1.dp,
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 2.dp, vertical = 1.dp)
        ) {
            modes.forEach { mode ->
                Surface(
                    shape = MaterialTheme.shapes.extraSmall,
                    color = if (currentMode == mode) 
                        MaterialTheme.colorScheme.primaryContainer 
                    else 
                        MaterialTheme.colorScheme.surface,
                    tonalElevation = if (currentMode == mode) 2.dp else 0.dp,
                    modifier = Modifier
                        .weight(1f)
                        .padding(1.dp)
                ) {
                    TextButton(
                        onClick = { onModeChanged(mode) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = if (currentMode == mode)
                                MaterialTheme.colorScheme.onPrimaryContainer
                            else
                                MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    ) {
                        Text(
                            text = mode.displayName,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }
}