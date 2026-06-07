package com.aandios.nous.feature.trades.ui

import com.aandios.nous.api.market.model.SymbolInfo
import com.aandios.nous.api.market.model.trades.Trade
import com.aandios.nous.core.domain.repository.SymbolInfoRepository
import com.aandios.nous.core.domain.repository.TradesRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

/**
 * Фильтр размера сделки.
 * Значения генерируются на основе minQty из SymbolInfo.
 */
enum class SizeFilter(val label: String) {
    All("All"),
    MinQty("≥ min"),
    MinQtyx10("≥ ×10"),
    MinQtyx100("≥ ×100"),
}

sealed class TradesState {
    data object Loading : TradesState()
    data class Connected(val trades: List<Trade>) : TradesState()
    data class Error(val message: String) : TradesState()
}

class TradesViewModel(
    private val tradesRepository: TradesRepository,
    private val symbolInfoRepository: SymbolInfoRepository? = null,
) {
    private val viewModelScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var subscriptionJob: Job? = null

    private val _state = MutableStateFlow<TradesState>(TradesState.Loading)
    val state: StateFlow<TradesState> = _state.asStateFlow()

    private val maxTrades = 100

    // Символы, загруженные через symbolInfoRepository
    private val _loadedSymbols = MutableStateFlow<List<SymbolInfo>>(emptyList())
    val loadedSymbols: StateFlow<List<SymbolInfo>> = _loadedSymbols.asStateFlow()

    // Информация о текущем символе (minQty, tickSize и т.д.)
    private val _currentSymbolInfo = MutableStateFlow<SymbolInfo?>(null)
    val currentSymbolInfo: StateFlow<SymbolInfo?> = _currentSymbolInfo.asStateFlow()

    // Минимальный размер сделки из SymbolInfo
    val minTradeSize: Double? get() = _currentSymbolInfo.value?.minQty

    // Выбранный фильтр размера
    private val _selectedSizeFilter = MutableStateFlow(SizeFilter.All)
    val selectedSizeFilter: StateFlow<SizeFilter> = _selectedSizeFilter.asStateFlow()

    private var currentSymbol: String = ""

    init {
        // Загружаем список символов при старте
        loadSymbols()
    }

    /**
     * Фильтрует список сделок по выбранному размеру.
     */
    private fun filterTrades(trades: List<Trade>): List<Trade> {
        val filter = _selectedSizeFilter.value
        val minQty = minTradeSize ?: return trades

        return when (filter) {
            SizeFilter.All -> trades
            SizeFilter.MinQty -> trades.filter { it.quantity >= minQty }
            SizeFilter.MinQtyx10 -> trades.filter { it.quantity >= minQty * 10 }
            SizeFilter.MinQtyx100 -> trades.filter { it.quantity >= minQty * 100 }
        }
    }

    fun updateSizeFilter(filter: SizeFilter) {
        _selectedSizeFilter.value = filter
        // Переприменяем фильтр к текущему стейту
        val currentState = _state.value
        if (currentState is TradesState.Connected) {
            // Просто обновляем состояние, чтобы триггернуть рекомпозицию
            _state.value = TradesState.Connected(currentState.trades)
        }
    }

    fun subscribeToTrades(symbol: String) {
        if (symbol == currentSymbol && _state.value is TradesState.Connected) return
        currentSymbol = symbol

        subscriptionJob?.cancel()
        _state.value = TradesState.Loading

        // Загружаем SymbolInfo для нового символа
        fetchSymbolInfo(symbol)

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

    /**
     * Подписывается на сделки для переданного symbol и сразу возвращает
     * отфильтрованный список (если active filter != All).
     */
    fun getFilteredTrades(allTrades: List<Trade>): List<Trade> {
        return filterTrades(allTrades)
    }

    private fun loadSymbols() {
        if (symbolInfoRepository == null) return

        viewModelScope.launch {
            try {
                val allSymbols = symbolInfoRepository?.getAllSymbolsInfo() ?: emptyList()
                val tradingSymbols = allSymbols
                    .filter { it.status == "TRADING" }
                    .sortedBy { it.symbol }
                if (tradingSymbols.isNotEmpty()) {
                    _loadedSymbols.value = tradingSymbols
                }
            } catch (e: Exception) {
                println("⚠️ TradesVM: Failed to load symbols: ${e.message}")
            }
        }
    }

    private fun fetchSymbolInfo(symbol: String) {
        if (symbolInfoRepository == null) return

        viewModelScope.launch {
            try {
                val symbolInfo = symbolInfoRepository.getSymbolInfo(symbol)
                _currentSymbolInfo.value = symbolInfo
            } catch (e: Exception) {
                println("❌ TradesVM: Failed to fetch symbolInfo for $symbol: ${e.message}")
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

    fun formatPrice(price: Double): String = fmt.formatPrice(price)

    fun formatQuantity(quantity: Double): String = fmt.formatVolume(quantity)

    fun clear() {
        viewModelScope.coroutineContext.cancelChildren()
        viewModelScope.cancel()
    }

    companion object {
        private val fmt = com.aandios.nous.core.ui.format.SymbolFormatter.DEFAULT
    }
}
