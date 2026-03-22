package com.aandios.nous.feature.dom.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DomHeader(
    symbol: String,
    timestamp: Long,
    currentMode: DomMode,
    onModeChanged: (DomMode) -> Unit,
    modifier: Modifier = Modifier.Companion
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Левая часть: символ и время
            Column(
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "DOM - $symbol",
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.labelMedium
                )
                if (timestamp > 0) {
                    val timeStr = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
                        .format(Date(timestamp))
                    Text(
                        text = timeStr,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 10.sp
                    )
                }
            }

            // Центр: индикатор Live
            Text(
                text = "● Live",
                color = Color.Green,
                style = MaterialTheme.typography.labelSmall
            )

            // Правая часть: переключатель режимов
            DomModeSwitch(
                currentMode = currentMode,
                onModeChanged = onModeChanged,
                modifier = Modifier.padding(start = 8.dp)
            )
        }
    }
}