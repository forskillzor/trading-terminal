package com.aandios.nous_platform.ui.dom
import com.aandios.nous.core.ui.format.SymbolFormatter

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
import com.aandios.nous_platform.domain.commands.CommandResult
import com.aandios.nous_platform.domain.commands.BuyMarketCommand
import com.aandios.nous_platform.domain.commands.SellMarketCommand
import com.aandios.nous_platform.domain.commands.BuyLimitCommand
import com.aandios.nous_platform.domain.commands.SellLimitCommand
import com.aandios.nous_platform.domain.commands.BuyBestBidCommand
import com.aandios.nous_platform.domain.commands.SellBestAskCommand
import com.aandios.nous_platform.domain.commands.TradeOffCommand
import com.aandios.nous_platform.domain.entities.OrderBook
import com.aandios.nous_platform.ui.components.TerminalButton

@Composable
fun OrderPlacementPanel(
    orderBook: OrderBook,
    selectedPrice: Double?,
    orderQuantity: String,
    onQuantityChanged: (String) -> Unit,
    onTradingCommand: (TradingCommand) -> Unit,
    onCommandResult: (CommandResult) -> Unit,
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
                    onClick = {
                        val symbol = orderBook.symbol
                        val quantity = orderQuantity.toDoubleOrNull() ?: 0.0
                        val command = BuyMarketCommand(symbol, quantity, onCommandResult)
                        onTradingCommand(command)
                    },
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
                    onClick = {
                        val symbol = orderBook.symbol
                        val quantity = orderQuantity.toDoubleOrNull() ?: 0.0
                        val command = SellMarketCommand(symbol, quantity, onCommandResult)
                        onTradingCommand(command)
                    },
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
                        if (selectedPrice != null) {
                            val symbol = orderBook.symbol
                            val quantity = orderQuantity.toDoubleOrNull() ?: 0.0
                            val command = BuyLimitCommand(symbol, selectedPrice, quantity, onCommandResult)
                            onTradingCommand(command)
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
                        if (selectedPrice != null) {
                            val symbol = orderBook.symbol
                            val quantity = orderQuantity.toDoubleOrNull() ?: 0.0
                            val command = SellLimitCommand(symbol, selectedPrice, quantity, onCommandResult)
                            onTradingCommand(command)
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
                        val bestBid = orderBook.bids.firstOrNull()?.price?.toDoubleOrNull() ?: 0.0
                        if (bestBid > 0) {
                            val symbol = orderBook.symbol
                            val quantity = orderQuantity.toDoubleOrNull() ?: 0.0
                            val command = BuyBestBidCommand(symbol, bestBid, quantity, onCommandResult)
                            onTradingCommand(command)
                        }
                    },
                    isActive = false,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = if (orderBook.bids.isNotEmpty()) "Best Bid" else "Best Bid (waiting...)",
                        color = if (orderBook.bids.isNotEmpty())
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        style = MaterialTheme.typography.labelSmall
                    )
                }

                TerminalButton(
                    onClick = {
                        val bestAsk = orderBook.asks.firstOrNull()?.price?.toDoubleOrNull() ?: 0.0
                        if (bestAsk > 0) {
                            val symbol = orderBook.symbol
                            val quantity = orderQuantity.toDoubleOrNull() ?: 0.0
                            val command = SellBestAskCommand(symbol, bestAsk, quantity, onCommandResult)
                            onTradingCommand(command)
                        }
                    },
                    isActive = false,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = if (orderBook.asks.isNotEmpty()) "Best Ask" else "Best Ask (waiting...)",
                        color = if (orderBook.asks.isNotEmpty())
                            MaterialTheme.colorScheme.secondary
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }

            // Trade Off кнопка
            TerminalButton(
                onClick = {
                    val command = TradeOffCommand(onCommandResult)
                    onTradingCommand(command)
                },
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

private fun formatPrice(price: Double): String = SymbolFormatter.DEFAULT.formatPrice(price)