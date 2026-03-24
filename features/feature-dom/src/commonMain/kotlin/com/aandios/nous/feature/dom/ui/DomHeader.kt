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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aandios.nous.feature.dom.domain.AggregationLevel
import com.aandios.nous.feature.dom.domain.SubscriptionDepth
import com.aandios.nous.feature.dom.domain.TradingProvider
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
    tradingProvider: TradingProvider = TradingProvider.BINANCE,
    onTradingProviderChanged: (TradingProvider) -> Unit = {},
    modifier: Modifier = Modifier.Companion
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 1.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            // Первая строка: провайдер, символ, время и индикатор Live
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Левая часть: провайдер и символ
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Выбор провайдера
                    TradingProviderDropdown(
                        currentProvider = tradingProvider,
                        onProviderChanged = onTradingProviderChanged,
                        modifier = Modifier
                    )
                    
                    // Вертикальный разделитель
                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(16.dp)
                            .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                    )
                    
                    // Символ (futures coin-m)
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = 1.dp,
                        modifier = Modifier.padding(vertical = 2.dp)
                    ) {
                        Text(
                            text = symbol,
                            color = MaterialTheme.colorScheme.onSurface,
                            style = MaterialTheme.typography.labelMedium,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                    
                    // Время обновления
                    if (timestamp > 0) {
                        val timeStr = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
                            .format(Date(timestamp))
                        Text(
                            text = timeStr,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 10.sp,
                            modifier = Modifier.padding(start = 4.dp)
                        )
                    }
                }

                // Правая часть: индикатор Live
                Text(
                    text = "● Live",
                    color = Color.Green,
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 11.sp
                )
            }

            // Вторая строка: настройки глубины, агрегации и режима DOM
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Левая часть: настройки глубины и агрегации
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 1.dp
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(1.dp),
                        modifier = Modifier.padding(horizontal = 3.dp, vertical = 1.dp)
                    ) {
                        // Выбор глубины подписки (компактный)
                        SubscriptionDepthDropdown(
                            currentDepth = subscriptionDepth,
                            onDepthChanged = onSubscriptionDepthChanged,
                            modifier = Modifier
                        )
                        
                        // Тонкий разделитель
                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .height(14.dp)
                                .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                        )
                        
                        // Выбор уровня агрегации (компактный)
                        AggregationLevelDropdown(
                            currentLevel = aggregationLevel,
                            onLevelChanged = onAggregationLevelChanged,
                            modifier = Modifier
                        )
                    }
                }

                // Правая часть: переключатель режимов DOM
                DomModeDropdown(
                    currentMode = currentMode,
                    onModeChanged = onModeChanged,
                    modifier = Modifier
                )
            }
        }
    }
}