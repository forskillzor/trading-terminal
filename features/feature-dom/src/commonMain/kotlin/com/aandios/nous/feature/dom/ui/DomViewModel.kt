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

    // ЕДИНЫЙ СТЕЙТ ВСЕХ НАСТРОЕК
    private val _domOptions = MutableStateFlow(DomOptions.default())
    val domOptions: StateFlow<DomOptions> = _domOptions.asStateFlow()

    // Остальные стейты (не влияют на подписку)
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

    private val _symbolTickSize = MutableStateFlow<Double?>(null)
    val symbolTickSize: StateFlow<Double?> = _symbolTickSize.asStateFlow()

    // Job для управления всеми подписками DOM (стакан, bookTicker, unified order book)
    // При изменении provider/symbol/depth — отменяется и создаётся новый

    init {
        // Загружаем tickSize для дефолтного символа при инициализации
        viewModelScope.launch {
            delay(500) // небольшая задержка, чтобы не блокировать старт
            val defaultSymbol = _domOptions.value.symbol.symbol
            fetchSymbolTickSize(defaultSymbol)
        }
        
        // Запускаем начальную подписку
        restartSubscription(_domOptions.value)
    }

    /**
     * Обновляет настройки DOM и при необходимости перезапускает подписку.
     */
    fun updateDomOptions(newOptions: DomOptions) {
        val oldOptions = _domOptions.value
        
        if (oldOptions != newOptions) {
            println("📊 VM: DomOptions updated")
            _domOptions.value = newOptions
            
            // Проверяем, изменились ли параметры, влияющие на подписку
            val subscriptionChanged = 
                oldOptions.provider != newOptions.provider ||
                oldOptions.symbol != newOptions.symbol ||
                oldOptions.depth != newOptions.depth ||
                oldOptions.mode != newOptions.mode
            
            if (subscriptionChanged) {
                restartSubscription(newOptions)
            }
            
            // Если изменился символ — обновляем tickSize
            if (oldOptions.symbol != newOptions.symbol) {
                fetchSymbolTickSize(newOptions.symbol.symbol)
            }
            

        }
    }

    private fun restartSubscription(options: DomOptions) {
        subscriptionJob?.cancel()
        println("🔄 VM: Restarting subscription with key ${options.subscriptionKey}, mode: ${options.mode}")
        
        subscriptionJob = viewModelScope.launch {
            when (options.mode) {
                DomMode.UNIFIED -> {
                    // В unified режиме используем только unifiedOrderBook
                    domRepository.subscribeToUnifiedOrderBook(
                        symbol = options.symbol.symbol,
                        depth = options.depth.value
                    ).catch { e ->
                        println("❌ Unified Order Book Error: ${e.message}")
                        e.printStackTrace()
                    }.collect { unifiedData ->
                        _unifiedOrderBook.value = unifiedData
                        // Обновляем orderBook для совместимости (например, OrderPlacementPanel)
                        _orderBook.value = unifiedData.toOrderBook()
                        // bookTicker не обновляем - не используется в unified режиме
                    }
                }
                
                DomMode.SPLIT -> {
                    // В split режиме используем отдельные потоки orderBook и bookTicker
                    domRepository.subscribeToOrderBook(
                        symbol = options.symbol.symbol,
                        depth = options.depth.value
                    ).catch { e ->
                        println("❌ VM Error: ${e.message}")
                        e.printStackTrace()
                    }.collect { data ->
                        _orderBook.value = data
                    }
                    
                    domRepository.getBookTicker(options.symbol.symbol)
                        .catch { e ->
                            println("❌ BestPrices error: ${e.message}")
                        }
                        .collect { prices ->
                            _bookTicker.value = prices
                        }
                    
                    // unifiedOrderBook не используется в split режиме, можно очистить
                    _unifiedOrderBook.value = null
                }
            }
        }
    }

    // Удобные методы для UI (обёртки над updateDomOptions для обратной совместимости)
    fun updateProvider(provider: TradingProvider) {
        updateDomOptions(_domOptions.value.copy(provider = provider))
    }
    
    fun updateSymbol(symbol: TradingSymbol) {
        updateDomOptions(_domOptions.value.copy(symbol = symbol))
    }
    
    fun updateDepth(depth: DepthLimit) {
        updateDomOptions(_domOptions.value.copy(depth = depth))
    }
    
    fun updateAggregation(aggregation: AggregationLevel) {
        updateDomOptions(_domOptions.value.copy(aggregation = aggregation))
    }
    
    fun updateMode(mode: DomMode) {
        updateDomOptions(_domOptions.value.copy(mode = mode))
    }
    
    fun toggleCollapsed() {
        updateDomOptions(_domOptions.value.copy(collapsed = !_domOptions.value.collapsed))
    }

    // Старые методы подписки (оставляем для обратной совместимости, но они используют domOptions)
    fun subscribeToBookTicker(symbol: String) {
        // Просто обновляем символ в domOptions
        updateSymbol(TradingSymbol(symbol, symbol, provider = _domOptions.value.provider))
    }

    fun subscribeToOrderBook(symbol: String, depth: Int) {
        // Обновляем и символ, и глубину в domOptions
        updateDomOptions(_domOptions.value.copy(
            symbol = TradingSymbol(symbol, symbol, provider = _domOptions.value.provider),
            depth = DepthLimit.create(depth)
        ))
    }

    fun subscribeToOrderBook(symbol: String) {
        subscribeToOrderBook(symbol, _domOptions.value.depth.value)
    }

    fun subscribeToUnifiedOrderBook(symbol: String, depth: Int) {
        // Обновляем и символ, и глубину в domOptions
        updateDomOptions(_domOptions.value.copy(
            symbol = TradingSymbol(symbol, symbol, provider = _domOptions.value.provider),
            depth = DepthLimit.create(depth)
        ))
    }

    fun subscribeToUnifiedOrderBook(symbol: String) {
        subscribeToUnifiedOrderBook(symbol, _domOptions.value.depth.value)
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
        if (closestLevel != null && _domOptions.value.aggregation != closestLevel) {
            println("📊 VM: Auto-updating aggregation level to ${closestLevel.displayName()} based on tickSize $tickSize")
            updateAggregation(closestLevel)
        }
    }

    /**
     * Возвращает агрегированный UnifiedOrderBook с применением текущего уровня агрегации.
     * Если unifiedOrderBook отсутствует, возвращает null.
     */
    val aggregatedUnifiedOrderBook: UnifiedOrderBook?
        get() = _unifiedOrderBook.value?.aggregate(_domOptions.value.aggregation)

    fun clear() {
        subscriptionJob?.cancel()
        viewModelScope.coroutineContext.cancelChildren()
    }
}