package com.aandios.nous_platform.ui.trades

import com.aandios.nous_platform.data.api.binance.models.Trade
import com.aandios.nous_platform.domain.repository.TradesRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.text.SimpleDateFormat
import java.util.*

class TradesViewModel(
    private val tradesRepository: TradesRepository
) {
    private val viewModelScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var subscriptionJob: Job? = null

    private val _trades = MutableStateFlow<List<Trade>>(emptyList())
    val trades: StateFlow<List<Trade>> = _trades.asStateFlow()

    private val maxTrades = 100 // Максимальное количество отображаемых сделок

    fun subscribeToTrades(symbol: String) {
        subscriptionJob?.cancel()

        subscriptionJob = viewModelScope.launch {
            tradesRepository.getTradesStream(symbol)
                .catch { e ->
                    println("Trades subscription error: ${e.message}")
                }
                .collect { trade ->
                    // Добавляем новую сделку в начало списка
                    val updatedTrades = listOf(trade) + _trades.value.take(maxTrades - 1)
                    _trades.value = updatedTrades
                }
        }
    }

    fun formatTime(timestamp: Long): String {
        val date = Date(timestamp)
        val formatter = SimpleDateFormat("HH:mm:ss")
        return formatter.format(date)
    }

    fun formatPrice(price: Double): String {
        return when {
            price >= 1000 -> String.format("%.2f", price)
            price >= 100 -> String.format("%.3f", price)
            price >= 10 -> String.format("%.4f", price)
            price >= 1 -> String.format("%.5f", price)
            else -> String.format("%.6f", price)
        }
    }

    fun formatQuantity(quantity: Double): String {
        return String.format("%.3f", quantity)
    }

    fun clear() {
        viewModelScope.coroutineContext.cancelChildren()
        viewModelScope.cancel()
    }
}