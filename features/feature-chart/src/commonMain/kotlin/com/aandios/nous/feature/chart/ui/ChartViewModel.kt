package com.aandios.nous.feature.chart.ui

import com.aandios.nous.api.market.adapters.SymbolInfoAdapter
import com.aandios.nous.api.market.model.Candle
import com.aandios.nous.core.domain.repository.ChartRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlin.coroutines.cancellation.CancellationException

class ChartViewModel(
    private val chartRepository: ChartRepository,
    private val symbolInfoAdapter: SymbolInfoAdapter,
) {
    private val viewModelScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var currentJob: Job? = null

    private val _chartState = MutableStateFlow<ChartState>(ChartState.Loading)
    val chartState: StateFlow<ChartState> = _chartState.asStateFlow()

    private val _currentSymbol = MutableStateFlow("BTCUSDT")
    val currentSymbol: StateFlow<String> = _currentSymbol.asStateFlow()

    private val _currentTimeframe = MutableStateFlow("1h")
    val currentTimeframe: StateFlow<String> = _currentTimeframe.asStateFlow()

    private val _symbols = MutableStateFlow<List<String>>(listOf("BTCUSDT", "ETHUSDT"))
    val symbols: StateFlow<List<String>> = _symbols.asStateFlow()

    init {
        loadSymbols()
    }

    private fun loadSymbols() {
        viewModelScope.launch {
            try {
                val allSymbols = symbolInfoAdapter.getAllSymbolsInfo()
                val tradingSymbols = allSymbols
                    .filter { it.status == "TRADING" }
                    .map { it.symbol }
                    .sorted()
                if (tradingSymbols.isNotEmpty()) {
                    _symbols.value = tradingSymbols
                }
            } catch (e: Exception) {
                println("Failed to load symbols: ${e.message}")
            }
        }
    }

    fun selectSymbol(symbol: String) {
        _currentSymbol.value = symbol
        loadChart(ticker = symbol, timeframe = _currentTimeframe.value)
    }

    fun selectTimeframe(timeframe: String) {
        _currentTimeframe.value = timeframe
        loadChart(ticker = _currentSymbol.value, timeframe = timeframe)
    }

    fun loadChart(ticker: String = "BTCUSDT", timeframe: String = "1h") {
        viewModelScope.launch {
            _chartState.value = ChartState.Loading

            delay(100)
            currentJob?.cancel()

            if (_chartState.value !is ChartState.Loading) {
                _chartState.value = ChartState.Loading
            }

            currentJob = launch {
                try {
                    chartRepository.getChart(ticker, timeframe)
                        .catch { e ->
                            println("ChartViewModel catch error: ${e.message}")
                            _chartState.value = ChartState.Error(e.message ?: "Unknown error")
                        }
                        .collect { candles ->
                            if (candles.isNotEmpty()) {
                                val lastPrice = candles.last().close
                                _chartState.value = ChartState.Success(
                                    candles = candles,
                                    currentPrice = lastPrice
                                )
                            }
                        }
                } catch (e: CancellationException) {
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

sealed interface ChartState {
    object Loading : ChartState
    data class Success(
        val candles: List<Candle>,
        val currentPrice: Float? = null
    ) : ChartState
    data class Error(val message: String) : ChartState
}
