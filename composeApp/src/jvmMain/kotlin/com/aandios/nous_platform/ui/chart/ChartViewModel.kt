package com.aandios.nous_platform.ui.chart

import com.aandios.nous_platform.domain.entities.Candle
import com.aandios.nous_platform.domain.usecases.GetChartByTickerUseCase
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlin.coroutines.cancellation.CancellationException

// ViewModel для desktop
class ChartViewModel(
    private val getChartUseCase: GetChartByTickerUseCase
) {
    private val viewModelScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var currentJob: Job? = null

    private val _chartState = MutableStateFlow<ChartState>(ChartState.Loading)
    val chartState: StateFlow<ChartState> = _chartState.asStateFlow()

    fun loadChart(ticker: String = "BTCUSDT", timeframe: String = "1h") {
        // Отменяем предыдущую подписку НЕМНОГО ПОЗЖЕ
        // Даем WebSocket время инициализироваться
        viewModelScope.launch {
            _chartState.value = ChartState.Loading

            // Небольшая задержка перед отменой старого job
            delay(100) // Даем 100ms для корректного завершения
            currentJob?.cancel()

            // Сбрасываем состояние на Loading только один раз
            if (_chartState.value !is ChartState.Loading) {
                _chartState.value = ChartState.Loading
            }

            currentJob = launch {
                try {
                    getChartUseCase(ticker, timeframe)
                        .catch { e ->
                            println("ViewModel catch error: ${e.message}")
                            _chartState.value = ChartState.Error(e.message ?: "Unknown error")
                        }
                        .collect { candles ->
                            // Только если это не пустой список (WebSocket еще не дал данные)
                            if (candles.isNotEmpty()) {
                                val lastPrice = candles.last().close
                                _chartState.value = ChartState.Success(
                                    candles = candles,
                                    currentPrice = lastPrice
                                )
                            }
                        }
                } catch (e: CancellationException) {
                    // Это нормально - просто отмена
                    println("Job cancelled: ${e.message}")
                } catch (e: Exception) {
                    println("Job error: ${e.message}")
                    _chartState.value = ChartState.Error(e.message ?: "Unknown error")
                }
            }
        }
    }

    fun clear() {
        viewModelScope.coroutineContext.cancelChildren()
        viewModelScope.cancel()
    }
}

// Состояния
sealed interface ChartState {
    object Loading : ChartState
    data class Success(
        val candles: List<Candle>,
        val currentPrice: Float? = null
    ) : ChartState
    data class Error(val message: String) : ChartState
}