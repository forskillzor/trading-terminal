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
import com.aandios.nous.feature.dom.domain.SubscriptionDepth

/**
 * Выпадающий список для выбора количества уровней подписки на стакан заявок.
 * Отображает текущее количество уровней и позволяет выбрать из предопределённых значений.
 *
 * @param currentDepth текущий выбранный уровень глубины
 * @param onDepthChanged callback при изменении глубины
 * @param modifier Modifier для контейнера
 */
@Composable
fun SubscriptionDepthDropdown(
    currentDepth: SubscriptionDepth,
    onDepthChanged: (SubscriptionDepth) -> Unit,
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
                    text = currentDepth.displayName,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 0.2.sp
                )
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = "Раскрыть список уровней глубины",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        // Выпадающее меню
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.width(120.dp)
        ) {
            SubscriptionDepth.all().forEach { depth ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = depth.displayName,
                            fontSize = 12.sp,
                            fontWeight = if (currentDepth == depth) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    onClick = {
                        onDepthChanged(depth)
                        expanded = false
                    }
                )
            }
        }
    }
}