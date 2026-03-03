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
import com.aandios.tradingterminal.data.api.binance.models.BestPrices

@Composable
fun DomSpread(
    bestPrices: BestPrices?,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = modifier
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (bestPrices != null) {
                // Best Bid
                Column(
                    horizontalAlignment = Alignment.Start
                ) {
                    Text(
                        text = "BID",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.labelSmall
                    )
                    Text(
                        text = formatPrice(bestPrices.bestBid),
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = formatQuantity(bestPrices.bestBidQty),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.labelSmall
                    )
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
                    Text(
                        text = formatPrice(bestPrices.spread),
                        color = Color.Yellow,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = String.format("(%.3f%%)", bestPrices.spreadPercent),
                        color = Color.Yellow,
                        style = MaterialTheme.typography.labelSmall
                    )
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
                    Text(
                        text = formatPrice(bestPrices.bestAsk),
                        color = MaterialTheme.colorScheme.secondary,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = formatQuantity(bestPrices.bestAskQty),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            } else {
                // Loading state
                Text(
                    text = "Loading...",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth().wrapContentWidth(Alignment.CenterHorizontally)
                )
            }
        }
    }
}

private fun formatPrice(price: Double): String {
    return when {
        price >= 1000 -> String.format("%.2f", price)
        price >= 100 -> String.format("%.3f", price)
        price >= 10 -> String.format("%.4f", price)
        price >= 1 -> String.format("%.5f", price)
        else -> String.format("%.6f", price)
    }
}

private fun formatQuantity(qty: Double): String {
    return when {
        qty >= 1000 -> String.format("%.2fk", qty / 1000)
        qty >= 100 -> String.format("%.1f", qty)
        qty >= 10 -> String.format("%.2f", qty)
        qty >= 1 -> String.format("%.3f", qty)
        else -> String.format("%.4f", qty)
    }
}