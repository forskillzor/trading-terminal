package com.aandios.nous.feature.chart.ui

import com.aandios.nous.api.market.adapters.SymbolInfoAdapter
import com.aandios.nous.api.market.adapters.TradesAdapter
import com.aandios.nous.api.market.model.Candle
import com.aandios.nous.api.market.model.FootprintCandle
import com.aandios.nous.api.market.model.FootprintLevel
import com.aandios.nous.api.market.model.MutableFootprintCandle
import com.aandios.nous.api.market.model.SymbolInfo
import com.aandios.nous.core.domain.repository.ChartRepository
import com.aandios.nous.core.storage.StateStore
import com.aandios.nous.core.ui.format.SymbolFormatter
import com.aandios.nous.feature.chart.footprint.FootprintApiClient
import com.aandios.nous.feature.dom.domain.model.AggregationLevel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlin.coroutines.cancellation.CancellationException

class ChartViewModel(
    private val chartRepository: ChartRepository,
    private val symbolInfoAdapter: SymbolInfoAdapter,
    private val footprintApiClient: FootprintApiClient? = null,
    private val tradesAdapter: TradesAdapter? = null,
    private val stateStore: StateStore? = null,
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

    private val _fpAggregation = MutableStateFlow<AggregationLevel>(AggregationLevel.BaseTick)

    // For UI observation
    val footprintCandles: StateFlow<List<FootprintCandle>> = _completedFootprintCandles
    val liveFootprintCandle: StateFlow<FootprintCandle?> = _liveFootprintCandle
    val footprintCurrentPrice: StateFlow<Float?> = _footprintCurrentPrice
    val footprintLoading: StateFlow<Boolean> = _footprintLoading
    val footprintError: StateFlow<String?> = _footprintError
    val chartMode: StateFlow<ChartMode> = _chartMode
    val symbolsWithFootprint: StateFlow<Set<String>> = _symbolsWithFootprint
    val fpAggregation: StateFlow<AggregationLevel> = _fpAggregation

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
        saveState()
        loadChart(ticker = symbol, timeframe = _currentTimeframe.value)
    }

    fun selectTimeframe(timeframe: String) {
        _currentTimeframe.value = timeframe
        saveState()
        loadChart(ticker = _currentSymbol.value, timeframe = timeframe)
    }

    fun toggleChartMode() {
        val newMode = when (_chartMode.value) {
            ChartMode.CANDLESTICK -> ChartMode.FOOTPRINT
            ChartMode.FOOTPRINT -> ChartMode.CANDLESTICK
        }
        _chartMode.value = newMode
        saveState()
        if (newMode == ChartMode.FOOTPRINT) {
            startLiveFootprint()
        } else {
            stopLiveFootprint()
        }
    }

    fun setFpAggregation(level: AggregationLevel) {
        _fpAggregation.value = level
        saveState()
    }

    private fun saveState() {
        val store = stateStore ?: return
        viewModelScope.launch {
            store.putString("chart_symbol", _currentSymbol.value)
            store.putString("chart_timeframe", _currentTimeframe.value)
            store.putString("chart_mode", _chartMode.value.name)
            store.putString("fp_aggregation", when (_fpAggregation.value) {
                AggregationLevel.BaseTick -> "BaseTick"
                AggregationLevel.TenTick -> "TenTick"
                AggregationLevel.HundredTick -> "HundredTick"
            })
        }
    }

    fun restoreState() {
        val store = stateStore ?: return
        viewModelScope.launch {
            store.getString("chart_symbol")?.let { _currentSymbol.value = it }
            store.getString("chart_timeframe")?.let { _currentTimeframe.value = it }
            store.getString("chart_mode")?.let { mode ->
                _chartMode.value = try { ChartMode.valueOf(mode) } catch (e: Exception) { ChartMode.CANDLESTICK }
            }
            store.getString("fp_aggregation")?.let { agg ->
                _fpAggregation.value = try { AggregationLevel.fromString(agg) } catch (e: Exception) { AggregationLevel.BaseTick }
            }
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

    // Load historical footprint from server, aggregates if needed
    private suspend fun fetchHistoricalFootprint(): List<FootprintCandle> {
        if (footprintApiClient == null) return emptyList()
        val (sourceTf, aggCount) = resolveFootprintSourceTimeframe(_currentTimeframe.value)
        return try {
            val raw = footprintApiClient.getFootprint(
                symbol = _currentSymbol.value,
                timeframe = sourceTf,
                limit = when (sourceTf) {
                    "1m" -> 20 * aggCount
                    "15m" -> 20 * aggCount
                    else -> 20
                }
            ).reversed() // server returns DESC, we store ASC
            if (aggCount > 1) aggregateFootprintCandles(raw, aggCount) else raw
        } catch (e: Exception) { emptyList() }
    }

    // Fetch one completed candle from server
    private suspend fun fetchCompletedCandle(startTime: Long, endTime: Long): FootprintCandle? {
        if (footprintApiClient == null) return null
        val (sourceTf, aggCount) = resolveFootprintSourceTimeframe(_currentTimeframe.value)
        val sourceStart = if (aggCount > 1) {
            // For aggregated timeframes, need to fetch multiple source candles
            null
        } else {
            startTime
        }
        val sourceEnd = if (aggCount > 1) null else endTime
        return try {
            val raw = footprintApiClient.getFootprint(
                symbol = _currentSymbol.value,
                timeframe = sourceTf,
                from = sourceStart,
                to = sourceEnd,
                limit = if (aggCount > 1) aggCount else 1
            ).reversed()
            if (aggCount > 1 && raw.size >= aggCount) {
                aggregateFootprintCandles(raw, aggCount).firstOrNull()
            } else {
                raw.firstOrNull()
            }
        } catch (e: Exception) { null }
    }

    fun startLiveFootprint() {
        if (tradesAdapter == null && footprintApiClient == null) {
            _footprintError.value = "Neither trades adapter nor footprint API available"
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

        val (sourceTf, aggCount) = resolveFootprintSourceTimeframe(_currentTimeframe.value)
        val displayTf = _currentTimeframe.value
        val isLiveTrades = sourceTf == "1m" && tradesAdapter != null
        val sourceMs = when (sourceTf) {
            "1m" -> 60_000L
            "15m" -> 900_000L
            else -> 60_000L
        }

        footprintJob = viewModelScope.launch {
            try {
                // 1. Load history from server
                val history = fetchHistoricalFootprint()
                _completedFootprintCandles.value = history
                _footprintLoading.value = false

                if (isLiveTrades) {
                    // ---- Live trade accumulation (1m or 5m) ----
                    val displayMs = when (displayTf) {
                        "1m" -> 60_000L; "5m" -> 300_000L; else -> 60_000L
                    }

                    val liveCandle = MutableFootprintCandle(
                        symbol = _currentSymbol.value,
                        startTime = 0L, endTime = 0L
                    )
                    var lastCandleStart = 0L
                    var tickCount = 0L

                    tradesAdapter.subscribeToTrades(_currentSymbol.value).collect { trade ->
                        val candleStart = trade.timestamp / displayMs * displayMs

                        if (lastCandleStart > 0L && candleStart != lastCandleStart) {
                            val completed = liveCandle.toFootprintCandle(tickCount)
                            _completedFootprintCandles.value = _completedFootprintCandles.value + completed

                            // For 1m: fetch authoritative version from server
                            if (sourceTf == "1m") {
                                val serverCandle = fetchCompletedCandle(lastCandleStart, candleStart)
                                if (serverCandle != null && serverCandle.levels.isNotEmpty()) {
                                    val updated = _completedFootprintCandles.value.toMutableList()
                                    updated[updated.lastIndex] = serverCandle
                                    _completedFootprintCandles.value = updated
                                }
                            }

                            liveCandle.clear()
                            liveCandle.addTrade(trade.price.toFloat(), trade.quantity.toFloat(), !trade.isBuyerMaker)
                            tickCount = 1
                        } else {
                            liveCandle.addTrade(trade.price.toFloat(), trade.quantity.toFloat(), !trade.isBuyerMaker)
                            tickCount++
                        }

                        lastCandleStart = candleStart

                        val liveSnapshot = liveCandle.toFootprintCandle(tickCount)
                        _liveFootprintCandle.value = if (liveSnapshot.levels.isNotEmpty()) liveSnapshot else null
                        _footprintCurrentPrice.value = liveCandle.lastPrice.takeIf { it > 0f }
                    }
                } else {
                    // ---- Server polling (15m, 30m, 1h, 4h) ----
                    // For 15m source: poll periodically for latest completed candle
                    while (isActive) {
                        delay(sourceMs) // wait one source period
                        if (_chartMode.value != ChartMode.FOOTPRINT) break

                        val now = System.currentTimeMillis()
                        val completedEnd = now / sourceMs * sourceMs
                        val completedStart = completedEnd - sourceMs * aggCount

                        // Fetch source candles and aggregate
                        if (footprintApiClient != null) {
                            val raw = footprintApiClient.getFootprint(
                                symbol = _currentSymbol.value,
                                timeframe = sourceTf,
                                from = completedStart - sourceMs,
                                to = completedEnd,
                                limit = aggCount + 2
                            ).reversed()
                            if (raw.isNotEmpty()) {
                                val aggregated = if (aggCount > 1) {
                                    aggregateFootprintCandles(raw, aggCount)
                                } else raw

                                val list = _completedFootprintCandles.value.toMutableList()
                                for (agg in aggregated) {
                                    val existingIdx = list.indexOfFirst { it.startTime == agg.startTime }
                                    if (existingIdx >= 0) list[existingIdx] = agg
                                    else list.add(agg)
                                }
                                _completedFootprintCandles.value = list.sortedBy { it.startTime }
                            }
                        }
                    }
                }
            } catch (e: CancellationException) {
                // normal stop
            } catch (e: Exception) {
                println("Live footprint error: ${e.message}")
                _footprintError.value = "Live footprint error: ${e.message}"
                _footprintLoading.value = false
            }
        }

        // Periodic server polling for latest completed candle (1m source only)
        if (sourceTf == "1m") {
            viewModelScope.launch {
                while (isActive) {
                    delay(60_000)
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
                val (sourceTf, aggCount) = resolveFootprintSourceTimeframe(_currentTimeframe.value)

                val historical = (footprintApiClient?.getFootprint(
                    symbol = _currentSymbol.value,
                    timeframe = sourceTf,
                    to = oldestTime - 1,
                    limit = 20 * aggCount
                ) ?: emptyList()).reversed() // server DESC → ASC

                if (historical.isEmpty()) {
                    _hasMoreFootprintHistory.value = false
                    isLoadingMoreFootprint = false
                    return@launch
                }

                val aggregated = if (aggCount > 1) aggregateFootprintCandles(historical, aggCount) else historical
                val newList = (aggregated + _completedFootprintCandles.value).distinctBy { it.startTime }
                _completedFootprintCandles.value = newList
                _footprintHistoryLoadCount.value = aggregated.size
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

    companion object {
        fun resolveFootprintSourceTimeframe(displayTimeframe: String): Pair<String, Int> {
            return when (displayTimeframe) {
                "1m" -> "1m" to 1
                "5m" -> "1m" to 5
                "15m" -> "15m" to 1
                "30m" -> "15m" to 2
                "1h" -> "15m" to 4
                "4h" -> "15m" to 16
                "1d" -> "15m" to 96
                "1w" -> "15m" to 672
                else -> "1m" to 1
            }
        }

        fun aggregateFootprintCandles(candles: List<FootprintCandle>, count: Int): List<FootprintCandle> {
            if (count <= 1 || candles.isEmpty()) return candles

            data class Acc(var bidVol: Float = 0f, var askVol: Float = 0f, var bidCnt: Int = 0, var askCnt: Int = 0)

            return candles.chunked(count)
                .filter { it.isNotEmpty() }
                .map { group ->
                    val startTime = group.first().startTime
                    val endTime = group.last().endTime
                    val minPrice = group.minOfOrNull { it.minPrice.toDoubleOrNull() ?: Double.MAX_VALUE } ?: 0.0
                    val maxPrice = group.maxOfOrNull { it.maxPrice.toDoubleOrNull() ?: Double.MIN_VALUE } ?: 0.0
                    val totalTicks = group.sumOf { it.totalTicks }

                    val levelMap = linkedMapOf<String, Acc>()
                    for (candle in group) {
                        for (level in candle.levels) {
                            val acc = levelMap.getOrPut(level.price) { Acc() }
                            acc.bidVol += level.bidVolumeFloat
                            acc.askVol += level.askVolumeFloat
                            acc.bidCnt += level.bidCount
                            acc.askCnt += level.askCount
                        }
                    }

                    val sorted = levelMap.entries.sortedByDescending { it.key.toDoubleOrNull() ?: 0.0 }
                    val levels = sorted.map { (price, acc) ->
                        FootprintLevel(
                            price = price,
                            bidVolume = acc.bidVol.toString(),
                            askVolume = acc.askVol.toString(),
                            bidCount = acc.bidCnt,
                            askCount = acc.askCnt
                        )
                    }
                    FootprintCandle(
                        exchange = group.first().exchange,
                        symbol = group.first().symbol,
                        timeframe = group.first().timeframe,
                        startTime = startTime,
                        endTime = endTime,
                        totalTicks = totalTicks,
                        minPrice = minPrice.toString(),
                        maxPrice = maxPrice.toString(),
                        levels = levels
                    )
                }
        }
    }
}

sealed interface ChartState {
    object Loading : ChartState
    data class Success(val candles: List<Candle>, val currentPrice: Float? = null) : ChartState
    data class Error(val message: String) : ChartState
}
