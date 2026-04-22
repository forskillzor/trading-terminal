package com.aandios.nous.feature.dom.ui

import com.aandios.nous.api.market.commands.*
import com.aandios.nous.api.market.model.BookTicker
import com.aandios.nous.api.market.model.orderbook.OrderBook
import com.aandios.nous.core.domain.repository.DomRepository
import com.aandios.nous.core.domain.repository.SymbolInfoRepository
import com.aandios.nous.feature.dom.data.repository.DomRepositoryImpl
import com.aandios.nous.feature.dom.domain.DomOptions
import com.aandios.nous.feature.dom.domain.UnifiedOrderBook
import com.aandios.nous.feature.dom.domain.model.DomEvent
import com.aandios.nous.feature.dom.domain.model.OrderIntent
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
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

    // Инкрементальные данные DOM
    private val _domEvents = MutableStateFlow<List<DomEvent>>(emptyList())
    val domEvents: StateFlow<List<DomEvent>> = _domEvents.asStateFlow()
    
    // Константы для управления памятью
    private companion object {
        const val MAX_DOM_EVENTS = 50
    }
    
    private val _incrementalBids = MutableStateFlow<Map<Double, Double>>(emptyMap())
    val incrementalBids: StateFlow<Map<Double, Double>> = _incrementalBids.asStateFlow()
    
    private val _incrementalAsks = MutableStateFlow<Map<Double, Double>>(emptyMap())
    val incrementalAsks: StateFlow<Map<Double, Double>> = _incrementalAsks.asStateFlow()
    
    private val _incrementalBestBid = MutableStateFlow<Double?>(null)
    val incrementalBestBid: StateFlow<Double?> = _incrementalBestBid.asStateFlow()
    
    private val _incrementalBestAsk = MutableStateFlow<Double?>(null)
    val incrementalBestAsk: StateFlow<Double?> = _incrementalBestAsk.asStateFlow()
    
    private val _incrementalBestBidQuantity = MutableStateFlow<Double?>(null)
    val incrementalBestBidQuantity: StateFlow<Double?> = _incrementalBestBidQuantity.asStateFlow()
    
    private val _incrementalBestAskQuantity = MutableStateFlow<Double?>(null)
    val incrementalBestAskQuantity: StateFlow<Double?> = _incrementalBestAskQuantity.asStateFlow()

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
        println("🔄 VM: Restarting subscription with key ${options.subscriptionKey}, mode: ${options.mode}, depth: ${options.depth.value}")
        
        subscriptionJob = viewModelScope.launch {
            subscribeToIncrementalDom(options)
        }
    }
    
    /**
     * Подписывается на инкрементальные события DOM и обновляет соответствующие StateFlow.
     */
    private suspend fun subscribeToIncrementalDom(options: DomOptions) {
        val domRepositoryImpl = domRepository as DomRepositoryImpl
        
        // Сбрасываем инкрементальные данные
        _incrementalBids.value = emptyMap()
        _incrementalAsks.value = emptyMap()
        _incrementalBestBid.value = null
        _incrementalBestAsk.value = null
        _incrementalBestBidQuantity.value = null
        _incrementalBestAskQuantity.value = null
        
        domRepositoryImpl.subscribeToDomEvents(
            symbol = options.symbol.symbol,
            depth = options.depth.value
        ).catch { e ->
            println("❌ DOM Events Error: ${e.message}")
            e.printStackTrace()
        }.collect { event ->
            processDomEvent(event)
        }
    }
    
    /**
     * Обрабатывает событие DomEvent и обновляет соответствующие StateFlow.
     */
    private fun processDomEvent(event: DomEvent) {
        when (event) {
            is DomEvent.Snapshot -> {
                // Очищаем текущие данные и загружаем из снапшота
                val bids = mutableMapOf<Double, Double>()
                val asks = mutableMapOf<Double, Double>()
                
                event.snapshot.bids.forEach { (priceStr, qtyStr) ->
                    val price = priceStr.toDoubleOrNull()
                    val quantity = qtyStr.toDoubleOrNull()
                    
                    if (price == null || quantity == null) {
                        println("⚠️ DomViewModel: Failed to parse snapshot bid data: price='$priceStr', quantity='$qtyStr'")
                        return@forEach
                    }
                    
                    if (quantity > 0.0) bids[price] = quantity
                }
                
                event.snapshot.asks.forEach { (priceStr, qtyStr) ->
                    val price = priceStr.toDoubleOrNull()
                    val quantity = qtyStr.toDoubleOrNull()
                    
                    if (price == null || quantity == null) {
                        println("⚠️ DomViewModel: Failed to parse snapshot ask data: price='$priceStr', quantity='$qtyStr'")
                        return@forEach
                    }
                    
                    if (quantity > 0.0) asks[price] = quantity
                }
                
                _incrementalBids.value = bids
                _incrementalAsks.value = asks
            }
            
            is DomEvent.UpdateBid -> {
                val currentBids = _incrementalBids.value.toMutableMap()
                if (event.quantity == 0.0) {
                    currentBids.remove(event.price)
                } else {
                    currentBids[event.price] = event.quantity
                }
                _incrementalBids.value = currentBids
            }
            
            is DomEvent.UpdateAsk -> {
                val currentAsks = _incrementalAsks.value.toMutableMap()
                if (event.quantity == 0.0) {
                    currentAsks.remove(event.price)
                } else {
                    currentAsks[event.price] = event.quantity
                }
                _incrementalAsks.value = currentAsks
            }
            
            is DomEvent.BestPrices -> {
                _incrementalBestBid.value = event.bestBid
                _incrementalBestAsk.value = event.bestAsk
                _incrementalBestBidQuantity.value = event.bestBidQuantity
                _incrementalBestAskQuantity.value = event.bestAskQuantity
            }
            
            DomEvent.Reset -> {
                _incrementalBids.value = emptyMap()
                _incrementalAsks.value = emptyMap()
                _incrementalBestBid.value = null
                _incrementalBestAsk.value = null
                _incrementalBestBidQuantity.value = null
                _incrementalBestAskQuantity.value = null
            }
        }
        
        // Обновляем список событий для отладки (ограничиваем размер)
        val currentEvents = _domEvents.value
        val newEvents = if (currentEvents.size >= MAX_DOM_EVENTS) {
            // Используем более эффективный подход: создаем новый список с удалением первого элемента
            currentEvents.drop(1) + event
        } else {
            currentEvents + event
        }
        _domEvents.value = newEvents
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

    fun selectPrice(price: Double?) {
        if (_selectedPrice.value != price) {
            println("💰 VM: Price selected: $price")
            _selectedPrice.value = price
        }
    }

    fun updateOrderQuantity(quantity: String) {
        _orderQuantity.value = quantity
    }
    
    fun handleOrderIntent(intent: OrderIntent) {
        val command = when (intent) {
            is OrderIntent.MarketBuy -> BuyMarketCommand(intent.symbol, intent.quantity) { result ->
                _lastCommandResult.value = result
            }
            is OrderIntent.MarketSell -> SellMarketCommand(intent.symbol, intent.quantity) { result ->
                _lastCommandResult.value = result
            }
            is OrderIntent.LimitBuy -> BuyLimitCommand(intent.symbol, intent.price, intent.quantity) { result ->
                _lastCommandResult.value = result
            }
            is OrderIntent.LimitSell -> SellLimitCommand(intent.symbol, intent.price, intent.quantity) { result ->
                _lastCommandResult.value = result
            }
            is OrderIntent.BestBidBuy -> BuyBestBidCommand(intent.symbol, intent.bestBidPrice, intent.quantity) { result ->
                _lastCommandResult.value = result
            }
            is OrderIntent.BestAskSell -> SellBestAskCommand(intent.symbol, intent.bestAskPrice, intent.quantity) { result ->
                _lastCommandResult.value = result
            }
            OrderIntent.ToggleTrading -> TradeOffCommand { result ->
                _lastCommandResult.value = result
            }
        }
        executeCommand(command)
    }
    
    private fun fetchSymbolTickSize(symbol: String) {
        if (symbolInfoRepository == null) return
        
        viewModelScope.launch {
            try {
                val symbolInfo = symbolInfoRepository.getSymbolInfo(symbol)
                val tickSize = symbolInfo?.tickSize
                _symbolTickSize.value = tickSize
                // Уровень агрегации сохраняется (multiplier), displayName обновится автоматически через UI
            } catch (e: Exception) {
                println("❌ Failed to fetch tickSize for $symbol: ${e.message}")
            }
        }
    }
}