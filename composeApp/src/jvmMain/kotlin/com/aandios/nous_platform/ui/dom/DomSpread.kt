package com.aandios.nous_platform.ui.dom
import com.aandios.nous.core.ui.format.SymbolFormatter

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.aandios.nous_platform.data.api.binance.models.BestPrices

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

private fun formatPrice(price: Double): String = SymbolFormatter.DEFAULT.formatPrice(price)

private fun formatQuantity(qty: Double): String = SymbolFormatter.DEFAULT.formatVolume(qty)