package com.aandios.nous.feature.dom.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.aandios.nous.feature.dom.domain.DomOptions
import com.aandios.nous.feature.dom.ui.header.DomHeader

@Composable
fun DomWidget(
    modifier: Modifier = Modifier,
    width: Dp = 300.dp,
    showHeader: Boolean = true,
    domOptions: DomOptions = DomOptions.default(),
    symbolTickSize: Double? = null,
    onDomOptionsChanged: (DomOptions) -> Unit = {},
    content: @Composable () -> Unit = { }
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .widthIn(min = width)
            .background(MaterialTheme.colorScheme.surface)
    ) {
        if (showHeader) {
            DomHeader(
                domOptions = domOptions,
                symbolTickSize = symbolTickSize,
                onDomOptionsChanged = onDomOptionsChanged,
                isLive = true
            )
        }
        Box(modifier = Modifier.weight(1f)) {
            content()
        }
    }
}
