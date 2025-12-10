package com.aandios.tradingterminal.ui.chart

import com.aandios.tradingterminal.domain.entities.Candle
import com.aandios.tradingterminal.domain.usecases.GetChartByTickerUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// ПРОСТАЯ ViewModel для десктопа
class ChartViewModel(
    private val getChartUseCase: GetChartByTickerUseCase
) {
    private val viewModelScope = CoroutineScope(Dispatchers.IO + Job())

    private val _chartState = MutableStateFlow<ChartState>(ChartState.Loading)
    val chartState: StateFlow<ChartState> = _chartState.asStateFlow()

    fun loadChart(ticker: String = "BTCUSDT", timeframe: String = "1h") {
        viewModelScope.launch {
            _chartState.value = ChartState.Loading
            try {
                getChartUseCase(ticker, timeframe).collect { candles ->
                    _chartState.value = ChartState.Success(candles)
                }
            } catch (e: Exception) {
                _chartState.value = ChartState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun clear() {
        viewModelScope.coroutineContext.cancelChildren()
    }
}

// Состояния
sealed interface ChartState {
    object Loading : ChartState
    data class Success(val candles: List<Candle>) : ChartState
    data class Error(val message: String) : ChartState
}