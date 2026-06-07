package com.aandios.nous.feature.chart.ui

import com.aandios.nous.api.market.adapters.SymbolInfoAdapter
import com.aandios.nous.api.market.adapters.TradesAdapter
import com.aandios.nous.api.market.model.Candle
import com.aandios.nous.api.market.model.FootprintCandle
import com.aandios.nous.api.market.model.MutableFootprintCandle
import com.aandios.nous.api.market.model.SymbolInfo
import com.aandios.nous.core.domain.repository.ChartRepository
import com.aandios.nous.core.ui.format.SymbolFormatter
import com.aandios.nous.feature.chart.footprint.FootprintApiClient
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlin.coroutines.cancellation.CancellationException

class ChartViewModel(
    private val chartRepository: ChartRepository,
    private val symbolInfoAdapter: SymbolInfoAdapter,
    private val footprintApiClient: FootprintApiClient? = null,
    private val tradesAdapter: TradesAdapter? = null,
) {
    private val viewModelScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var currentJob: Job? = null
    private var footprintJob: Job? = null
    private var isLoadingMore = false

    private val _chartState = MutableStateFlow<ChartState>(ChartState.Loading)
    val chartState: StateFlow<ChartState> = _chartState.asStateFlow()

    private val _currentSymbol = MutableStateFlow("BTCUSDT")
    val currentSymbol: StateFlow<String> = _currentSymbol.asStateFlow()

    private val _currentTimeframe = MutableStateFlow("1h")
    val currentTimeframe: StateFlow<String> = _currentTimeframe.asStateFlow()

    private val _symbols = MutableStateFlow<List<String>>(listOf("BTCUSDT", "ETHUSDT"))
    val symbols: StateFlow<List<String>> = _symbols.asStateFlow()

    private val _symbolInfoMap = MutableStateFlow<Map<String, SymbolInfo>>(emptyMap())
    private val _currentSymbolFormatter = MutableStateFlow(SymbolFormatter())
    val currentSymbolFormatter: StateFlow<SymbolFormatter> = _currentSymbolFormatter.asStateFlow()

    private val _historyLoadCount = MutableStateFlow(0)
    val historyLoadCount: StateFlow<Int> = _historyLoadCount.asStateFlow()

    private val _hasMoreHistory = MutableStateFlow(true)
    val hasMoreHistory: StateFlow<Boolean> = _hasMoreHistory.asStateFlow()

    // Footprint state
    private val _completedFootprintCandles = MutableStateFlow<List<FootprintCandle>>(emptyList())
    private val _liveFootprintCandle = MutableStateFlow<FootprintCandle?>(null)
    private val _footprintCurrentPrice = MutableStateFlow<Float?>(null)
    private val _footprintLoading = MutableStateFlow(false)
    private val _footprintError = MutableStateFlow<String?>(null)
    private val _chartMode = MutableStateFlow(ChartMode.CANDLESTICK)
    private val _symbolsWithFootprint = MutableStateFlow<Set<String>>(emptySet())

    // For UI observation
    val footprintCandles: StateFlow<List<FootprintCandle>> = _completedFootprintCandles
    val liveFootprintCandle: StateFlow<FootprintCandle?> = _liveFootprintCandle
    val footprintCurrentPrice: StateFlow<Float?> = _footprintCurrentPrice
    val footprintLoading: StateFlow<Boolean> = _footprintLoading
    val footprintError: StateFlow<String?> = _footprintError
    val chartMode: StateFlow<ChartMode> = _chartMode
    val symbolsWithFootprint: StateFlow<Set<String>> = _symbolsWithFootprint

    // Footprint pagination
    private val _hasMoreFootprintHistory = MutableStateFlow(true)
    val hasMoreFootprintHistory: StateFlow<Boolean> = _hasMoreFootprintHistory
    private val _footprintHistoryLoadCount = MutableStateFlow(0)
    val footprintHistoryLoadCount: StateFlow<Int> = _footprintHistoryLoadCount
    private var isLoadingMoreFootprint = false

    init {
        loadSymbols()
        loadFootprintSymbols()
    }

    private fun loadSymbols() {
        viewModelScope.launch {
            try {
                val allSymbols = symbolInfoAdapter.getAllSymbolsInfo()
                val trading = allSymbols.filter { it.status == "TRADING" }
                val map = trading.associateBy { it.symbol }
                _symbolInfoMap.value = map
                _symbols.value = trading.map { it.symbol }.sorted()
                // Set formatter for current symbol
                map[_currentSymbol.value]?.let {
                    _currentSymbolFormatter.value = SymbolFormatter(it.tickSize, it.minQty)
                }
            } catch (e: Exception) {
                println("Failed to load symbols: ${e.message}")
            }
        }
    }

    fun selectSymbol(symbol: String) {
        _currentSymbol.value = symbol
        _symbolInfoMap.value[symbol]?.let {
            _currentSymbolFormatter.value = SymbolFormatter(it.tickSize, it.minQty)
        }
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
            startLiveFootprint()
        } else {
            stopLiveFootprint()
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

    // Load historical footprint from server (returns ASC order: oldest first)
    private suspend fun fetchHistoricalFootprint(): List<FootprintCandle> {
        if (footprintApiClient == null) return emptyList()
        return try {
            footprintApiClient.getFootprint(
                symbol = _currentSymbol.value,
                timeframe = "1m",
                limit = 20
            ).reversed() // server returns DESC, we store ASC
        } catch (e: Exception) { emptyList() }
    }

    // Fetch one completed candle from server
    private suspend fun fetchCompletedCandle(startTime: Long, endTime: Long): FootprintCandle? {
        if (footprintApiClient == null) return null
        return try {
            footprintApiClient.getFootprint(
                symbol = _currentSymbol.value,
                timeframe = "1m",
                from = startTime,
                to = endTime,
                limit = 1
            ).firstOrNull()
        } catch (e: Exception) { null }
    }

    fun startLiveFootprint() {
        if (tradesAdapter == null) {
            _footprintError.value = "Trades adapter not available for live footprint"
            loadFootprintData()
            return
        }

        footprintJob?.cancel()
        _footprintLoading.value = true
        _footprintError.value = null
        _completedFootprintCandles.value = emptyList()
        _liveFootprintCandle.value = null
        _hasMoreFootprintHistory.value = true
        _footprintHistoryLoadCount.value = 0
        isLoadingMoreFootprint = false

        footprintJob = viewModelScope.launch {
            try {
                // 1. Load history from server
                val history = fetchHistoricalFootprint()
                _completedFootprintCandles.value = history
                _footprintLoading.value = false

                // 2. Start live accumulation
                val liveCandle = MutableFootprintCandle(
                    symbol = _currentSymbol.value,
                    startTime = 0L, // will be set on first trade
                    endTime = 0L
                )
                var lastCandleStart = 0L
                var tickCount = 0L

                tradesAdapter.subscribeToTrades(_currentSymbol.value).collect { trade ->
                    val candleStart = trade.timestamp / 60_000 * 60_000

                    // Minute boundary: finish current candle, fetch from server
                    if (lastCandleStart > 0L && candleStart != lastCandleStart) {
                        val completed = liveCandle.toFootprintCandle(tickCount)
                        _completedFootprintCandles.value = _completedFootprintCandles.value + completed

                        // Fetch authoritative version from server
                        val serverCandle = fetchCompletedCandle(lastCandleStart, candleStart)
                        if (serverCandle != null && serverCandle.levels.isNotEmpty()) {
                            val updated = _completedFootprintCandles.value.toMutableList()
                            updated[updated.lastIndex] = serverCandle
                            _completedFootprintCandles.value = updated
                            // Schedule another fetch after a few seconds (in case server data came late)
                            launch {
                                delay(3000)
                                val checkAgain = fetchCompletedCandle(lastCandleStart, candleStart)
                                if (checkAgain != null && checkAgain.levels.isNotEmpty() && checkAgain.totalTicks > (serverCandle.totalTicks)) {
                                    val list = _completedFootprintCandles.value.toMutableList()
                                    list[list.lastIndex] = checkAgain
                                    _completedFootprintCandles.value = list
                                }
                            }
                        }

                        // Start new candle — clear old levels
                        liveCandle.clear()
                        liveCandle.addTrade(trade.price.toFloat(), trade.quantity.toFloat(), !trade.isBuyerMaker)
                        tickCount = 1
                    } else {
                        liveCandle.addTrade(trade.price.toFloat(), trade.quantity.toFloat(), !trade.isBuyerMaker)
                        tickCount++
                    }

                    lastCandleStart = candleStart

                    // Update live candle state
                    val liveSnapshot = liveCandle.toFootprintCandle(tickCount)
                    _liveFootprintCandle.value = if (liveSnapshot.levels.isNotEmpty()) liveSnapshot else null
                    _footprintCurrentPrice.value = liveCandle.lastPrice.takeIf { it > 0f }
                }
            } catch (e: CancellationException) {
                // normal stop
            } catch (e: Exception) {
                println("Live footprint error: ${e.message}")
                _footprintError.value = "Live footprint error: ${e.message}"
                _footprintLoading.value = false
            }
        }

        // Periodic server polling for latest completed candle
        viewModelScope.launch {
            while (isActive) {
                delay(60_000) // every minute
                if (_chartMode.value != ChartMode.FOOTPRINT) break
                val now = System.currentTimeMillis()
                val completedStart = (now / 60_000 * 60_000) - 60_000
                val completedEnd = completedStart + 60_000
                val serverCandle = fetchCompletedCandle(completedStart, completedEnd)
                if (serverCandle != null && serverCandle.levels.isNotEmpty()) {
                    val list = _completedFootprintCandles.value.toMutableList()
                    if (list.isNotEmpty() && list.last().startTime == completedStart) {
                        list[list.lastIndex] = serverCandle
                    } else {
                        list.add(serverCandle)
                    }
                    _completedFootprintCandles.value = list
                }
            }
        }
    }

    fun stopLiveFootprint() {
        footprintJob?.cancel()
        footprintJob = null
        _liveFootprintCandle.value = null
    }

    fun loadMoreFootprintHistory() {
        if (isLoadingMoreFootprint || !_hasMoreFootprintHistory.value) return
        isLoadingMoreFootprint = true

        viewModelScope.launch {
            try {
                val oldestTime = _completedFootprintCandles.value.firstOrNull()?.startTime ?: run {
                    isLoadingMoreFootprint = false; return@launch
                }

                val historical = (footprintApiClient?.getFootprint(
                    symbol = _currentSymbol.value,
                    timeframe = "1m",
                    to = oldestTime - 1,
                    limit = 20
                ) ?: emptyList()).reversed() // server DESC → ASC

                if (historical.isEmpty()) {
                    _hasMoreFootprintHistory.value = false
                    isLoadingMoreFootprint = false
                    return@launch
                }

                val newList = (historical + _completedFootprintCandles.value).distinctBy { it.startTime }
                _completedFootprintCandles.value = newList
                _footprintHistoryLoadCount.value = historical.size
            } catch (e: Exception) {
                println("Failed to load more footprint history: ${e.message}")
            } finally {
                isLoadingMoreFootprint = false
            }
        }
    }

    fun loadFootprintData() {
        footprintJob?.cancel()
        footprintJob = null
        _liveFootprintCandle.value = null

        viewModelScope.launch {
            _footprintLoading.value = true
            _footprintError.value = null
            val data = fetchHistoricalFootprint()
            _completedFootprintCandles.value = data
            _footprintLoading.value = false
            if (data.isEmpty()) {
                _footprintError.value = "No footprint data in DB"
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
                        .catch { e -> _chartState.value = ChartState.Error(e.message ?: "Unknown error") }
                        .collect { candles ->
                            if (candles.isNotEmpty()) {
                                val lastPrice = candles.last().close
                                _chartState.value = ChartState.Success(candles = candles, currentPrice = lastPrice)
                            }
                        }
                } catch (e: CancellationException) {
                    println("Job cancelled: ${e.message}")
                } catch (e: Exception) {
                    _chartState.value = ChartState.Error(e.message ?: "Unknown error")
                }
            }

            if (_chartMode.value == ChartMode.FOOTPRINT) {
                startLiveFootprint()
            }
        }
    }

    fun loadMoreHistory() {
        if (isLoadingMore || !_hasMoreHistory.value) return
        isLoadingMore = true

        viewModelScope.launch {
            try {
                val state = _chartState.value
                if (state !is ChartState.Success) { isLoadingMore = false; return@launch }

                val oldestTime = state.candles.firstOrNull()?.timestamp ?: run { isLoadingMore = false; return@launch }
                val endTime = oldestTime - 1

                val historicalCandles = chartRepository.loadHistoricalCandlesBefore(
                    ticker = _currentSymbol.value, timeframe = _currentTimeframe.value, endTime = endTime, limit = 200
                )
                if (historicalCandles.isEmpty()) { _hasMoreHistory.value = false; isLoadingMore = false; return@launch }

                val newCandles = historicalCandles + state.candles
                val lastPrice = newCandles.last().close

                currentJob?.cancel()
                _chartState.value = ChartState.Success(candles = newCandles, currentPrice = lastPrice)
                _historyLoadCount.value = historicalCandles.size
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
    data class Success(val candles: List<Candle>, val currentPrice: Float? = null) : ChartState
    data class Error(val message: String) : ChartState
}
