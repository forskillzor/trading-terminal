package com.aandios.nous.feature.trades.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aandios.nous.api.market.model.trades.Trade

private val BuyColor = Color(0xFF26A69A)      // зелёный — покупка
private val SellColor = Color(0xFFEF5350)     // красный — продажа
private val HeaderBg = Color(0xFF1E1E1E)
private val RowBgEven = Color(0xFF1A1A1A)
private val RowBgOdd = Color(0xFF141414)

@Composable
fun TradesWidget(
    viewModel: TradesViewModel,
    modifier: Modifier = Modifier
) {
    val trades by viewModel.trades.collectAsState()

    Column(modifier = modifier.background(MaterialTheme.colorScheme.background)) {
        // Заголовки
        HeaderRow()

        HorizontalDivider(thickness = 1.dp, color = Color(0xFF333333))

        if (trades.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Ожидание данных...",
                    color = Color.Gray,
                    fontSize = 13.sp
                )
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(trades, key = { it.id }) { trade ->
                    TradeRow(
                        trade = trade,
                        viewModel = viewModel,
                        index = trades.indexOf(trade)
                    )
                }
            }
        }
    }
}

@Composable
private fun HeaderRow() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(HeaderBg)
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        Text(
            text = "Время",
            color = Color.Gray,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1.2f)
        )
        Text(
            text = "Цена",
            color = Color.Gray,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = "Кол-во",
            color = Color.Gray,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(0.8f)
        )
    }
}

@Composable
private fun TradeRow(
    trade: Trade,
    viewModel: TradesViewModel,
    index: Int
) {
    val bgColor = if (index % 2 == 0) RowBgEven else RowBgOdd
    val priceColor = if (trade.isBuyerMaker) SellColor else BuyColor

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(bgColor)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Время
        Text(
            text = viewModel.formatTime(trade.timestamp),
            color = Color(0xFFB0B0B0),
            fontSize = 11.sp,
            modifier = Modifier.weight(1.2f)
        )

        // Цена
        Text(
            text = viewModel.formatPrice(trade.price),
            color = priceColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1f)
        )

        // Количество
        Text(
            text = viewModel.formatQuantity(trade.quantity),
            color = Color(0xFFB0B0B0),
            fontSize = 11.sp,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(0.8f)
        )
    }
}
