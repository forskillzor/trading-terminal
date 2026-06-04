package com.aandios.nous.feature.chart.ui

import com.aandios.nous.api.market.adapters.SymbolInfoAdapter
import com.aandios.nous.api.market.model.Candle
import com.aandios.nous.api.market.model.FootprintCandle
import com.aandios.nous.core.domain.repository.ChartRepository
import com.aandios.nous.feature.chart.footprint.FootprintApiClient
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlin.coroutines.cancellation.CancellationException

class ChartViewModel(
    private val chartRepository: ChartRepository,
    private val symbolInfoAdapter: SymbolInfoAdapter,
    private val footprintApiClient: FootprintApiClient? = null,
) {
    private val viewModelScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var currentJob: Job? = null
    private var isLoadingMore = false

    private val _chartState = MutableStateFlow<ChartState>(ChartState.Loading)
    val chartState: StateFlow<ChartState> = _chartState.asStateFlow()

    private val _currentSymbol = MutableStateFlow("BTCUSDT")
    val currentSymbol: StateFlow<String> = _currentSymbol.asStateFlow()

    private val _currentTimeframe = MutableStateFlow("1h")
    val currentTimeframe: StateFlow<String> = _currentTimeframe.asStateFlow()

    private val _symbols = MutableStateFlow<List<String>>(listOf("BTCUSDT", "ETHUSDT"))
    val symbols: StateFlow<List<String>> = _symbols.asStateFlow()

    private val _historyLoadCount = MutableStateFlow(0)
    val historyLoadCount: StateFlow<Int> = _historyLoadCount.asStateFlow()

    private val _hasMoreHistory = MutableStateFlow(true)
    val hasMoreHistory: StateFlow<Boolean> = _hasMoreHistory.asStateFlow()

    private val _footprintCandles = MutableStateFlow<List<FootprintCandle>>(emptyList())
    val footprintCandles: StateFlow<List<FootprintCandle>> = _footprintCandles.asStateFlow()

    private val _footprintLoading = MutableStateFlow(false)
    val footprintLoading: StateFlow<Boolean> = _footprintLoading.asStateFlow()

    private val _footprintError = MutableStateFlow<String?>(null)
    val footprintError: StateFlow<String?> = _footprintError.asStateFlow()

    private val _chartMode = MutableStateFlow(ChartMode.CANDLESTICK)
    val chartMode: StateFlow<ChartMode> = _chartMode.asStateFlow()

    private val _symbolsWithFootprint = MutableStateFlow<Set<String>>(emptySet())
    val symbolsWithFootprint: StateFlow<Set<String>> = _symbolsWithFootprint.asStateFlow()

    init {
        loadSymbols()
        loadFootprintSymbols()
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

    fun toggleChartMode() {
        val newMode = when (_chartMode.value) {
            ChartMode.CANDLESTICK -> ChartMode.FOOTPRINT
            ChartMode.FOOTPRINT -> ChartMode.CANDLESTICK
        }
        _chartMode.value = newMode
        if (newMode == ChartMode.FOOTPRINT) {
            loadFootprintData()
        }
    }

    private fun loadFootprintSymbols() {
        if (footprintApiClient == null) return
        viewModelScope.launch {
            try {
                val instruments = footprintApiClient.getInstruments()
                _symbolsWithFootprint.value = instruments.map { it.symbol }.toSet()
            } catch (e: Exception) {
                println("Failed to load footprint symbols: ${e.message}")
            }
        }
    }

    fun loadFootprintData() {
        if (footprintApiClient == null) {
            _footprintError.value = "Footprint API client not available"
            return
        }
        viewModelScope.launch {
            _footprintLoading.value = true
            _footprintError.value = null
            try {
                val data = footprintApiClient.getFootprint(
                    symbol = _currentSymbol.value,
                    timeframe = _currentTimeframe.value,
                    limit = 500
                )
                _footprintCandles.value = data
                if (data.isEmpty()) {
                    _footprintError.value = "No footprint data in DB"
                }
            } catch (e: Exception) {
                val msg = "Footprint load failed: ${e.message}"
                println(msg)
                e.printStackTrace()
                _footprintError.value = msg
            } finally {
                _footprintLoading.value = false
            }
        }
    }

    fun loadChart(ticker: String = "BTCUSDT", timeframe: String = "1h") {
        _hasMoreHistory.value = true
        _historyLoadCount.value = 0
        isLoadingMore = false

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

            // Auto-load footprint if in footprint mode
            if (_chartMode.value == ChartMode.FOOTPRINT) {
                loadFootprintData()
            }
        }
    }

    fun loadMoreHistory() {
        if (isLoadingMore || !_hasMoreHistory.value) return
        isLoadingMore = true

        viewModelScope.launch {
            try {
                val state = _chartState.value
                if (state !is ChartState.Success) {
                    isLoadingMore = false
                    return@launch
                }


                val oldestTime = state.candles.firstOrNull()?.timestamp ?: run {
                    isLoadingMore = false
                    return@launch
                }

                val endTime = oldestTime - 1

                val historicalCandles = chartRepository.loadHistoricalCandlesBefore(
                    ticker = _currentSymbol.value,
                    timeframe = _currentTimeframe.value,
                    endTime = endTime,
                    limit = 200
                )

                if (historicalCandles.isEmpty()) {
                    _hasMoreHistory.value = false
                    isLoadingMore = false
                    return@launch
                }

                val newCandles = historicalCandles + state.candles
                val lastPrice = newCandles.last().close
                val loadedCount = historicalCandles.size


                currentJob?.cancel()

                _chartState.value = ChartState.Success(
                    candles = newCandles,
                    currentPrice = lastPrice
                )
                _historyLoadCount.value = loadedCount
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isLoadingMore = false
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
