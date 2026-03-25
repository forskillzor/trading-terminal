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
import com.aandios.nous.feature.dom.domain.TradingProvider

/**
 * Выпадающий список для выбора торгового провайдера.
 * Отображает текущий провайдер и позволяет выбрать из доступных значений.
 *
 * @param currentProvider текущий выбранный провайдер
 * @param onProviderChanged callback при изменении провайдера
 * @param modifier Modifier для контейнера
 */
@Composable
fun TradingProviderDropdown(
    currentProvider: TradingProvider,
    onProviderChanged: (TradingProvider) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Box(
        modifier = modifier.wrapContentSize(Alignment.TopStart)
    ) {
        // Кнопка-триггер с текущим провайдером и стрелкой
        Surface(
            shape = MaterialTheme.shapes.small,
            color = MaterialTheme.colorScheme.surfaceVariant,
            tonalElevation = 2.dp,
            modifier = Modifier.clickable { expanded = true }
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.padding(horizontal = 0.dp, vertical = 0.dp)
            ) {
                Text(
                    text = currentProvider.displayName,
                    color = MaterialTheme.colorScheme.inverseOnSurface,
                    fontSize = MaterialTheme.typography.labelSmall.fontSize,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 0.2.sp,
                    maxLines = 1
                )
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = "Раскрыть список провайдеров",
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
            TradingProvider.all().forEach { provider ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = provider.displayName,
                            fontSize = 12.sp,
                            fontWeight = if (currentProvider == provider) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    onClick = {
                        onProviderChanged(provider)
                        expanded = false
                    }
                )
            }
        }
    }
}