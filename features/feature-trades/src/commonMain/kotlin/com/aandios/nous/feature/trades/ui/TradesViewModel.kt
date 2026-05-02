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

sealed class TradesState {
    data object Loading : TradesState()
    data class Connected(val trades: List<Trade>) : TradesState()
    data class Error(val message: String) : TradesState()
}

class TradesViewModel(
    private val tradesRepository: TradesRepository
) {
    private val viewModelScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var subscriptionJob: Job? = null

    private val _state = MutableStateFlow<TradesState>(TradesState.Loading)
    val state: StateFlow<TradesState> = _state.asStateFlow()

    private val maxTrades = 100

    private var currentSymbol: String = ""

    fun subscribeToTrades(symbol: String) {
        if (symbol == currentSymbol && _state.value is TradesState.Connected) return
        currentSymbol = symbol

        subscriptionJob?.cancel()
        _state.value = TradesState.Loading

        subscriptionJob = viewModelScope.launch {
            tradesRepository.getTradesStream(symbol)
                .catch { e ->
                    println("❌ Trades subscription error: ${e.message}")
                    _state.value = TradesState.Error("Ошибка: ${e.message}")
                }
                .collect { trade ->
                    val trades = listOf(trade) + (_state.value as? TradesState.Connected)?.trades.orEmpty().take(maxTrades - 1)
                    _state.value = TradesState.Connected(trades)
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
