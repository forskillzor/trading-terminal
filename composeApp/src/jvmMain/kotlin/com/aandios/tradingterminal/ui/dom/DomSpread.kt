package com.aandios.tradingterminal.ui.dom

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun DomSpread(
    bestBid: Double?,
    bestAsk: Double?,
    modifier: Modifier = Modifier.Companion
) {
    val spread = if (bestBid != null && bestAsk != null) {
        bestAsk - bestBid
    } else null

    val spreadPercent = if (bestBid != null && bestAsk != null && bestBid > 0) {
        (spread!! / bestBid) * 100
    } else null

    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.Companion
                .fillMaxSize()
                .padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Best Bid
            Column(
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = "BID",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelSmall
                )
                bestBid?.let {
                    Text(
                        text = formatDomPrice(it),
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            // Spread
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "SPREAD",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelSmall
                )
                spread?.let {
                    Text(
                        text = String.format("%.2f", it),
                        color = Color.Yellow,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                spreadPercent?.let {
                    Text(
                        text = String.format("(%.4f%%)", it),
                        color = Color.Companion.Yellow,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }

            // Best Ask
            Column(
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = "ASK",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelSmall
                )
                bestAsk?.let {
                    Text(
                        text = formatDomPrice(it),
                        color = MaterialTheme.colorScheme.secondary,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}