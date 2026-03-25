package com.aandios.nous.feature.dom.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aandios.nous.feature.dom.domain.DomMode

/**
 * Выпадающий список для выбора режима DOM.
 * Отображает текущий режим и позволяет выбрать из доступных значений: Classic и Ninja.
 *
 * @param currentMode текущий выбранный режим DOM
 * @param onModeChanged callback при изменении режима
 * @param modifier Modifier для контейнера
 */
@Composable
fun DomModeDropdown(
    currentMode: DomMode,
    onModeChanged: (DomMode) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Box(
        modifier = modifier.wrapContentSize(Alignment.TopStart)
    ) {
        // Кнопка-триггер с текущим режимом и стрелкой
        Surface(
            shape = MaterialTheme.shapes.small,
            color = MaterialTheme.colorScheme.surfaceVariant,
            tonalElevation = 2.dp,
            modifier = Modifier.clickable { expanded = true }
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = currentMode.displayName,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 0.2.sp
                )
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = "Раскрыть список режимов DOM",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        // Выпадающее меню
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.width(100.dp)
        ) {
            DomMode.values().forEach { mode ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = mode.displayName,
                            fontSize = 12.sp,
                            fontWeight = if (currentMode == mode) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    onClick = {
                        onModeChanged(mode)
                        expanded = false
                    }
                )
            }
        }
    }
}