package com.aandios.nous.feature.trades.ui

import com.aandios.nous.api.market.model.trades.Trade
import com.aandios.nous.core.domain.repository.TradesRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

class TradesViewModel(
    private val tradesRepository: TradesRepository
) {
    private val viewModelScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var subscriptionJob: Job? = null

    private val _trades = MutableStateFlow<List<Trade>>(emptyList())
    val trades: StateFlow<List<Trade>> = _trades.asStateFlow()

    private val maxTrades = 100

    fun subscribeToTrades(symbol: String) {
        subscriptionJob?.cancel()

        subscriptionJob = viewModelScope.launch {
            tradesRepository.getTradesStream(symbol)
                .catch { e ->
                    println("Trades subscription error: ${e.message}")
                }
                .collect { trade ->
                    val updatedTrades = listOf(trade) + _trades.value.take(maxTrades - 1)
                    _trades.value = updatedTrades
                }
        }
    }

    fun formatTime(timestamp: Long): String {
        val seconds = timestamp / 1000
        val hours = (seconds / 3600) % 24
        val minutes = (seconds / 60) % 60
        val secs = seconds % 60
        return "${hours.toString().padStart(2, '0')}:${minutes.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}"
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
