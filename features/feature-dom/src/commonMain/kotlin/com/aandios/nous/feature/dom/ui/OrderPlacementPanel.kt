package com.aandios.nous.feature.dom.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aandios.nous_platform.domain.commands.TradingCommand
import com.aandios.nous_platform.ui.components.TerminalButton

@Composable
fun OrderPlacementPanel(
    selectedPrice: Double?,
    orderQuantity: String,
    onQuantityChanged: (String) -> Unit,
    onCreateBuyMarket: () -> TradingCommand,
    onCreateSellMarket: () -> TradingCommand,
    onCreateBuyLimit: (() -> TradingCommand?)?,
    onCreateSellLimit: (() -> TradingCommand?)?,
    onCreateBuyBestBid: (() -> TradingCommand?)?,
    onCreateSellBestAsk: (() -> TradingCommand?)?,
    onCreateTradeOff: () -> TradingCommand,
    onExecuteCommand: (TradingCommand?) -> Unit,
    isTradingEnabled: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Статус торговли
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Trading:",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelSmall
                )
                Text(
                    text = if (isTradingEnabled) "ON" else "OFF",
                    color = if (isTradingEnabled) Color.Green else Color.Red,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold
                    )
                )
            }

            // Информация о позиции
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "PnL:",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelSmall
                )
                Text(
                    text = selectedPrice?.let { formatPrice(it) } ?: "--",
                    color = if (selectedPrice != null) Color.Yellow
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = FontFamily.Monospace
                    )
                )
            }

            // Поле ввода количества
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Qty:",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelSmall
                )
                BasicTextField(
                    value = orderQuantity,
                    onValueChange = onQuantityChanged,
                    modifier = Modifier
                        .weight(1f)
                        .height(28.dp)
                        .background(
                            color = MaterialTheme.colorScheme.surface,
                            shape = MaterialTheme.shapes.small
                        )
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.outlineVariant,
                            shape = MaterialTheme.shapes.small
                        )
                        .padding(horizontal = 6.dp, vertical = 4.dp),
                    textStyle = TextStyle(
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace
                    ),
                    singleLine = true,
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary)
                )
            }

            // Market ордера
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                TerminalButton(
                    onClick = { onExecuteCommand(onCreateBuyMarket()) },
                    isActive = false,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "Market Buy",
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.labelSmall
                    )
                }

                TerminalButton(
                    onClick = { onExecuteCommand(onCreateSellMarket()) },
                    isActive = false,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "Market Sell",
                        color = MaterialTheme.colorScheme.secondary,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }

            // Limit ордера (по выбранной цене)
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                TerminalButton(
                    onClick = {
                        if (onCreateBuyLimit != null && selectedPrice != null) {
                            onExecuteCommand(onCreateBuyLimit())
                        }
                    },
                    isActive = false,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = if (selectedPrice != null) "Buy Limit" else "Buy Limit (select price)",
                        color = if (selectedPrice != null)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        style = MaterialTheme.typography.labelSmall
                    )
                }

                TerminalButton(
                    onClick = {
                        if (onCreateSellLimit != null && selectedPrice != null) {
                            onExecuteCommand(onCreateSellLimit())
                        }
                    },
                    isActive = false,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = if (selectedPrice != null) "Sell Limit" else "Sell Limit (select price)",
                        color = if (selectedPrice != null)
                            MaterialTheme.colorScheme.secondary
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }

            // Best Bid/Ask ордера
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                TerminalButton(
                    onClick = {
                        onCreateBuyBestBid?.let { onExecuteCommand(it()) }
                    },
                    isActive = false,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = if (onCreateBuyBestBid != null) "Best Bid" else "Best Bid (waiting...)",
                        color = if (onCreateBuyBestBid != null)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        style = MaterialTheme.typography.labelSmall
                    )
                }

                TerminalButton(
                    onClick = {
                        onCreateSellBestAsk?.let { onExecuteCommand(it()) }
                    },
                    isActive = false,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = if (onCreateSellBestAsk != null) "Best Ask" else "Best Ask (waiting...)",
                        color = if (onCreateSellBestAsk != null)
                            MaterialTheme.colorScheme.secondary
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }

            // Trade Off кнопка
            TerminalButton(
                onClick = { onExecuteCommand(onCreateTradeOff()) },
                isActive = !isTradingEnabled,  // Активна когда торговля ВЫКЛЮЧЕНА
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = if (isTradingEnabled) "⚠️ TRADE OFF" else "✅ TRADE ON",
                    color = if (isTradingEnabled) Color.Red else Color.Green,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold
                    )
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