package com.aandios.nous.feature.dom.ui

import com.aandios.nous.api.market.commands.BuyBestBidCommand
import com.aandios.nous.api.market.commands.BuyLimitCommand
import com.aandios.nous.api.market.commands.BuyMarketCommand
import com.aandios.nous.api.market.commands.CommandResult
import com.aandios.nous.api.market.commands.SellBestAskCommand
import com.aandios.nous.api.market.commands.SellLimitCommand
import com.aandios.nous.api.market.commands.SellMarketCommand
import com.aandios.nous.api.market.commands.TradeOffCommand
import com.aandios.nous.api.market.commands.TradingCommand
import com.aandios.nous.api.market.model.BookTicker
import com.aandios.nous.api.market.model.orderbook.OrderBook
import com.aandios.nous.core.domain.repository.DomRepository
import com.aandios.nous.core.domain.repository.SymbolInfoRepository
import com.aandios.nous.feature.dom.data.repository.subscribeToUnifiedOrderBook
import com.aandios.nous.feature.dom.domain.*
import com.aandios.nous.feature.dom.domain.model.AggregationLevel
import com.aandios.nous.feature.dom.domain.model.DepthLimit
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.delay
import java.util.concurrent.Executors

class DomViewModel(
    private val domRepository: DomRepository,
    private val symbolInfoRepository: SymbolInfoRepository? = null,
) {
    private val dispatcher = Executors.newSingleThreadExecutor().asCoroutineDispatcher()
    private val viewModelScope = CoroutineScope(dispatcher + SupervisorJob())
    private var subscriptionJob: Job? = null

    private val _orderBook = MutableStateFlow<OrderBook?>(null)
    val orderBook: StateFlow<OrderBook?> = _orderBook.asStateFlow()

    private val _selectedPrice = MutableStateFlow<Double?>(null)
    val selectedPrice: StateFlow<Double?> = _selectedPrice.asStateFlow()

    private val _orderQuantity = MutableStateFlow("0.01")
    val orderQuantity: StateFlow<String> = _orderQuantity.asStateFlow()

    private val _isTradingEnabled = MutableStateFlow(true)
    val isTradingEnabled: StateFlow<Boolean> = _isTradingEnabled.asStateFlow()

    private val _lastCommandResult = MutableStateFlow<CommandResult?>(null)
    val lastCommandResult: StateFlow<CommandResult?> = _lastCommandResult.asStateFlow()

    private val _bookTicker = MutableStateFlow<BookTicker?>(null)
    val bookTicker: StateFlow<BookTicker?> = _bookTicker.asStateFlow()

    private val _unifiedOrderBook = MutableStateFlow<UnifiedOrderBook?>(null)
    val unifiedOrderBook: StateFlow<UnifiedOrderBook?> = _unifiedOrderBook.asStateFlow()

    private val _aggregationLevel = MutableStateFlow(AggregationLevel.TICK_0_1)
    val aggregationLevel: StateFlow<AggregationLevel> = _aggregationLevel.asStateFlow()

    private val _subscriptionDepth = MutableStateFlow(SubscriptionDepth.default())
    val subscriptionDepth: StateFlow<SubscriptionDepth> = _subscriptionDepth.asStateFlow()

    private val _tradingProvider = MutableStateFlow(TradingProvider.BINANCE)
    val tradingProvider: StateFlow<TradingProvider> = _tradingProvider.asStateFlow()

    private val _tradingSymbol = MutableStateFlow(TradingSymbol.defaultForProvider(TradingProvider.BINANCE))
    val tradingSymbol: StateFlow<TradingSymbol> = _tradingSymbol.asStateFlow()

    private val _depthLimit = MutableStateFlow(DepthLimit.default())
    val depthLimit: StateFlow<DepthLimit> = _depthLimit.asStateFlow()

    private val _collapsed = MutableStateFlow(false)
    val collapsed: StateFlow<Boolean> = _collapsed.asStateFlow()

    private val _domMode = MutableStateFlow(DomMode.CLASSIC)
    val domMode: StateFlow<DomMode> = _domMode.asStateFlow()

    private val _symbolTickSize = MutableStateFlow<Double?>(null)
    val symbolTickSize: StateFlow<Double?> = _symbolTickSize.asStateFlow()

    private var bookTickerJob: Job? = null
    private var unifiedSubscriptionJob: Job? = null

    init {
        // Загружаем tickSize для дефолтного символа при инициализации
        viewModelScope.launch {
            delay(500) // небольшая задержка, чтобы не блокировать старт
            val defaultSymbol = _tradingSymbol.value.symbol
            fetchSymbolTickSize(defaultSymbol)
        }
    }

    fun subscribeToBookTicker(symbol: String) {
        bookTickerJob?.cancel()

        bookTickerJob = viewModelScope.launch {
            domRepository.getBookTicker(symbol)
                .catch { e ->
                    println("❌ BestPrices error: ${e.message}")
                }
                .collect { prices ->
                    _bookTicker.value = prices
                }
        }
    }

    fun subscribeToOrderBook(symbol: String, depth: Int) {
        println("📊 VM: Subscribing to $symbol with depth=$depth")

        subscriptionJob?.cancel()

        subscriptionJob = viewModelScope.launch {
            domRepository.subscribeToOrderBook(symbol, depth)
                .catch { e ->
                    println("❌ VM Error: ${e.message}")
                    e.printStackTrace()
                }
                .collect { data ->
                    _orderBook.value = data
                }
        }
    }

    fun subscribeToOrderBook(symbol: String) {
        subscribeToOrderBook(symbol, _subscriptionDepth.value.levels)
    }

    fun subscribeToUnifiedOrderBook(symbol: String, depth: Int) {
        println("📊 VM: Subscribing to unified order book for $symbol with depth=$depth")

        unifiedSubscriptionJob?.cancel()

        unifiedSubscriptionJob = viewModelScope.launch {
            domRepository.subscribeToUnifiedOrderBook(symbol, depth)
                .catch { e ->
                    println("❌ Unified Order Book Error: ${e.message}")
                    e.printStackTrace()
                }
                .collect { unifiedData ->
                    _unifiedOrderBook.value = unifiedData
                }
        }
    }

    fun subscribeToUnifiedOrderBook(symbol: String) {
        subscribeToUnifiedOrderBook(symbol, _subscriptionDepth.value.levels)
    }

    // Единый метод для выполнения команд
    fun executeCommand(command: TradingCommand?) {
        if (command != null) {
            viewModelScope.launch {
                if (!_isTradingEnabled.value && command !is TradeOffCommand) {
                    _lastCommandResult.value = CommandResult.TradingDisabled
                    println("🔴 VM: Trading is OFF - command rejected: ${command.getDescription()}")
                    return@launch
                }

                if (!command.canExecute()) {
                    _lastCommandResult.value = CommandResult.Error("Cannot execute command: ${command.getDescription()}")
                    println("❌ VM: Command cannot execute: ${command.getDescription()}")
                    return@launch
                }

                println("📝 VM: Executing command: ${command.getDescription()}")
                command.execute()
            }
        }
    }

    // Фабричные методы для создания команд (удобно для UI)
    fun createBuyMarketCommand(): TradingCommand {
        val symbol = _orderBook.value?.symbol ?: "UNKNOWN"
        val quantity = _orderQuantity.value.toDoubleOrNull() ?: 0.0

        return BuyMarketCommand(symbol, quantity) { result ->
            _lastCommandResult.value = result
            println("📝 VM: Buy Market result: $result")
        }
    }

    fun createSellMarketCommand(): TradingCommand {
        val symbol = _orderBook.value?.symbol ?: "UNKNOWN"
        val quantity = _orderQuantity.value.toDoubleOrNull() ?: 0.0

        return SellMarketCommand(symbol, quantity) { result ->
            _lastCommandResult.value = result
            println("📝 VM: Sell Market result: $result")
        }
    }

    fun createBuyLimitCommand(): TradingCommand? {
        val symbol = _orderBook.value?.symbol ?: return null
        val price = _selectedPrice.value ?: return null
        val quantity = _orderQuantity.value.toDoubleOrNull() ?: return null

        return BuyLimitCommand(symbol, price, quantity) { result ->
            _lastCommandResult.value = result
            println("📝 VM: Buy Limit result: $result")
        }
    }

    fun createSellLimitCommand(): TradingCommand? {
        val symbol = _orderBook.value?.symbol ?: return null
        val price = _selectedPrice.value ?: return null
        val quantity = _orderQuantity.value.toDoubleOrNull() ?: return null

        return SellLimitCommand(symbol, price, quantity) { result ->
            _lastCommandResult.value = result
            println("📝 VM: Sell Limit result: $result")
        }
    }

    fun createBuyBestBidCommand(): TradingCommand? {
        val symbol = _orderBook.value?.symbol ?: return null
        val bestBid = _orderBook.value?.bids?.firstOrNull()?.price?.toDoubleOrNull() ?: return null
        val quantity = _orderQuantity.value.toDoubleOrNull() ?: return null

        return BuyBestBidCommand(symbol, bestBid, quantity) { result ->
            _lastCommandResult.value = result
            println("📝 VM: Buy Best Bid result: $result")
        }
    }

    fun createSellBestAskCommand(): TradingCommand? {
        val symbol = _orderBook.value?.symbol ?: return null
        val bestAsk = _orderBook.value?.asks?.firstOrNull()?.price?.toDoubleOrNull() ?: return null
        val quantity = _orderQuantity.value.toDoubleOrNull() ?: return null

        return SellBestAskCommand(symbol, bestAsk, quantity) { result ->
            _lastCommandResult.value = result
            println("📝 VM: Sell Best Ask result: $result")
        }
    }

    fun createTradeOffCommand(): TradingCommand {
        return TradeOffCommand { result ->
            if (result is CommandResult.Success) {
                _isTradingEnabled.value = !_isTradingEnabled.value
                println("🔴 VM: Trading is now ${if (_isTradingEnabled.value) "ON" else "OFF"}")
            }
            _lastCommandResult.value = result
        }
    }

    fun selectPrice(price: Double?) {
        if (_selectedPrice.value != price) {
            println("💰 VM: Price selected: $price")
            _selectedPrice.value = price
        }
    }

    fun updateOrderQuantity(quantity: String) {
        _orderQuantity.value = quantity
    }

    fun updateAggregationLevel(level: AggregationLevel) {
        if (_aggregationLevel.value != level) {
            println("📊 VM: Aggregation level changed to ${level.displayName()}")
            _aggregationLevel.value = level
        }
    }

    // todo VM: updateSubscriptionDepth need to use in header on select depth limit
    fun updateSubscriptionDepth(depth: SubscriptionDepth) {
        if (_subscriptionDepth.value != depth) {
            println("📊 VM: Subscription depth changed to ${depth.displayName}")
            _subscriptionDepth.value = depth
            
            // При смене глубины подписки перезапускаем подписку
            val currentSymbol = _tradingSymbol.value
            subscribeToOrderBook(currentSymbol.symbol, depth.levels)
            subscribeToUnifiedOrderBook(currentSymbol.symbol, depth.levels)
        }
    }

    fun updateTradingProvider(provider: TradingProvider) {
        if (_tradingProvider.value != provider) {
            println("📊 VM: Trading provider changed to ${provider.displayName}")
            _tradingProvider.value = provider
            
            // При смене провайдера обновляем символ на дефолтный для нового провайдера
            val defaultSymbol = TradingSymbol.defaultForProvider(provider)
            updateTradingSymbol(defaultSymbol)
        }
    }

    fun updateTradingSymbol(symbol: TradingSymbol) {
        if (_tradingSymbol.value != symbol) {
            println("📊 VM: Trading symbol changed to ${symbol.displayName}")
            _tradingSymbol.value = symbol
            
            // При смене символа перезапускаем подписку
            subscribeToOrderBook(symbol.symbol, _depthLimit.value.value)
            subscribeToBookTicker(symbol.symbol)
            
            // Получаем tickSize для символа и обновляем агрегацию
            fetchSymbolTickSize(symbol.symbol)
        }
    }
    
    private fun fetchSymbolTickSize(symbol: String) {
        if (symbolInfoRepository == null) return
        
        viewModelScope.launch {
            try {
                val symbolInfo = symbolInfoRepository.getSymbolInfo(symbol)
                val tickSize = symbolInfo?.tickSize
                _symbolTickSize.value = tickSize
                if (tickSize != null) {
                    updateAggregationFromTickSize(tickSize)
                }
            } catch (e: Exception) {
                println("❌ Failed to fetch tickSize for $symbol: ${e.message}")
            }
        }
    }
    
    private fun updateAggregationFromTickSize(tickSize: Double) {
        // Выбираем ближайший уровень агрегации из доступных
        val availableLevels = AggregationLevel.all()
        val closestLevel = availableLevels.minByOrNull { level -> 
            kotlin.math.abs(level.tickSize - tickSize) 
        }
        if (closestLevel != null && _aggregationLevel.value != closestLevel) {
            println("📊 VM: Auto-updating aggregation level to ${closestLevel.displayName()} based on tickSize $tickSize")
            _aggregationLevel.value = closestLevel
        }
    }

    fun updateDepthLimit(limit: DepthLimit) {
        if (_depthLimit.value != limit) {
            println("📊 VM: Depth limit changed to ${limit.value} levels")
            _depthLimit.value = limit
            
            // При смене глубины перезапускаем подписку
            val currentSymbol = _tradingSymbol.value
            subscribeToOrderBook(currentSymbol.symbol, limit.value)
        }
    }



    fun updateDomMode(mode: DomMode) {
        if (_domMode.value != mode) {
            println("📊 VM: DOM mode changed to ${mode.displayName}")
            _domMode.value = mode
        }
    }

    fun toggleCollapsed() {
        _collapsed.value = !_collapsed.value
        println("📊 VM: Header collapsed state changed to ${_collapsed.value}")
    }

    /**
     * Возвращает агрегированный UnifiedOrderBook с применением текущего уровня агрегации.
     * Если unifiedOrderBook отсутствует, возвращает null.
     */
    val aggregatedUnifiedOrderBook: UnifiedOrderBook?
        get() = _unifiedOrderBook.value?.aggregate(_aggregationLevel.value)

    fun clear() {
        subscriptionJob?.cancel()
        unifiedSubscriptionJob?.cancel()
        bookTickerJob?.cancel()
        viewModelScope.coroutineContext.cancelChildren()
    }
}