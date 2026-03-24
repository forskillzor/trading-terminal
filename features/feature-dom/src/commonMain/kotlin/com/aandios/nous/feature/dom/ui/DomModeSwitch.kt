package com.aandios.nous.feature.dom.ui

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aandios.nous.feature.dom.domain.DomMode

/**
 * Компактный переключатель между режимами Classic и Ninja DOM.
 * Стилизован как в профессиональных торговых терминалах: два сегмента с активной подсветкой.
 *
 * @param currentMode текущий выбранный режим
 * @param onModeChanged callback при изменении режима
 * @param modifier Modifier для контейнера
 */
@Composable
fun DomModeSwitch(
    currentMode: DomMode,
    onModeChanged: (DomMode) -> Unit,
    modifier: Modifier = Modifier
) {
    val segmentWidth = 70.dp
    val segmentHeight = 28.dp
    val cornerRadius = 6.dp

    // Анимация положения индикатора
    val indicatorOffset by animateDpAsState(
        targetValue = when (currentMode) {
            DomMode.CLASSIC -> 0.dp
            DomMode.NINJA -> segmentWidth
        },
        animationSpec = tween(durationMillis = 200),
        label = "indicatorOffset"
    )

    Box(
        modifier = modifier
            .width(segmentWidth * 2)
            .height(segmentHeight)
            .clip(RoundedCornerShape(cornerRadius))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant,
                shape = RoundedCornerShape(cornerRadius)
            )
    ) {
        // Активный индикатор (скользящий фон)
        Box(
            modifier = Modifier
                .width(segmentWidth)
                .height(segmentHeight)
                .offset(x = indicatorOffset)
                .clip(RoundedCornerShape(cornerRadius))
                .background(MaterialTheme.colorScheme.primary)
                .shadow(
                    elevation = 2.dp,
                    shape = RoundedCornerShape(cornerRadius),
                    clip = true
                )
        )

        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Сегмент Classic
            Box(
                modifier = Modifier
                    .width(segmentWidth)
                    .fillMaxHeight()
                    .clickable { onModeChanged(DomMode.CLASSIC) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Classic",
                    color = if (currentMode == DomMode.CLASSIC) {
                        MaterialTheme.colorScheme.onPrimary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 0.2.sp
                )
            }

            // Сегмент Ninja
            Box(
                modifier = Modifier
                    .width(segmentWidth)
                    .fillMaxHeight()
                    .clickable { onModeChanged(DomMode.NINJA) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Ninja",
                    color = if (currentMode == DomMode.NINJA) {
                        MaterialTheme.colorScheme.onPrimary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 0.2.sp
                )
            }
        }
    }
}