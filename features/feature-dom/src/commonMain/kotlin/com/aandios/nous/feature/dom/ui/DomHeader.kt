package com.aandios.nous.feature.dom.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aandios.nous.feature.dom.domain.AggregationLevel
import com.aandios.nous.feature.dom.domain.SubscriptionDepth
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DomHeader(
    symbol: String,
    timestamp: Long,
    currentMode: DomMode,
    onModeChanged: (DomMode) -> Unit,
    aggregationLevel: AggregationLevel,
    onAggregationLevelChanged: (AggregationLevel) -> Unit,
    subscriptionDepth: SubscriptionDepth = SubscriptionDepth.default(),
    onSubscriptionDepthChanged: (SubscriptionDepth) -> Unit = {},
    modifier: Modifier = Modifier.Companion
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 1.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Левая часть: символ и время
            Column(
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.weight(1f, fill = false)
            ) {
                Text(
                    text = "DOM • $symbol",
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.labelMedium,
                    maxLines = 1
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

            // Центр: индикатор Live с анимацией пульсации
            Text(
                text = "● Live",
                color = Color.Green,
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(horizontal = 8.dp)
            )

            // Правая часть: управление отображением DOM
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.weight(1.5f, fill = false)
            ) {
                // Группа настроек глубины и агрегации (визуально сгруппированы)
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 1.dp,
                    modifier = Modifier.padding(vertical = 2.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        // Выбор глубины подписки (количество уровней)
                        SubscriptionDepthDropdown(
                            currentDepth = subscriptionDepth,
                            onDepthChanged = onSubscriptionDepthChanged,
                            modifier = Modifier
                        )
                        
                        // Тонкий разделитель
                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .height(16.dp)
                                .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                        )
                        
                        // Выбор уровня агрегации
                        AggregationLevelDropdown(
                            currentLevel = aggregationLevel,
                            onLevelChanged = onAggregationLevelChanged,
                            modifier = Modifier
                        )
                    }
                }
                
                // Вертикальный разделитель между группами настроек
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(20.dp)
                        .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                )
                
                // Переключатель режимов DOM
                DomModeDropdown(
                    currentMode = currentMode,
                    onModeChanged = onModeChanged,
                    modifier = Modifier
                )
            }
        }
    }
}