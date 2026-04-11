package com.aandios.nous.feature.dom.ui.unified

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun UnifiedDomSpread(
    bestBid: Double,
    bestAsk: Double,
    modifier: Modifier = Modifier.Companion
) {
    val spread = bestAsk - bestBid
    val spreadPercent = (spread / bestBid) * 100

    Row(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = formatPrice(bestBid),
            color = MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.bodySmall.copy(
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "SPREAD",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelSmall
            )
            Text(
                text = "${formatPrice(spread)} (${"%.2f".format(spreadPercent)}%)",
                color = Color.Yellow,
                style = MaterialTheme.typography.labelSmall
            )
        }

        Text(
            text = formatPrice(bestAsk),
            color = MaterialTheme.colorScheme.secondary,
            style = MaterialTheme.typography.bodySmall.copy(
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
        )
    }
}