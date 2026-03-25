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
import com.aandios.nous.feature.dom.domain.AggregationLevel

/**
 * Выпадающий список для выбора уровня агрегации DOM.
 * Отображает текущий уровень и позволяет выбрать из предопределённых значений: 0.1, 1.0, 10.
 *
 * @param currentLevel текущий выбранный уровень агрегации
 * @param onLevelChanged callback при изменении уровня
 * @param modifier Modifier для контейнера
 */
@Composable
fun AggregationLevelDropdown(
    currentLevel: AggregationLevel,
    onLevelChanged: (AggregationLevel) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Box(
        modifier = modifier.wrapContentSize(Alignment.TopStart)
    ) {
        // Кнопка-триггер с текущим уровнем и стрелкой
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
                    text = currentLevel.displayName(),
                    color = MaterialTheme.colorScheme.inverseOnSurface,
                    fontSize = MaterialTheme.typography.labelSmall.fontSize,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 0.2.sp
                )
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = "Раскрыть список уровней агрегации",
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
            AggregationLevel.all().forEach { level ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = level.displayName(),
                            fontSize = 12.sp,
                            fontWeight = if (currentLevel == level) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    onClick = {
                        onLevelChanged(level)
                        expanded = false
                    }
                )
            }
        }
    }
}