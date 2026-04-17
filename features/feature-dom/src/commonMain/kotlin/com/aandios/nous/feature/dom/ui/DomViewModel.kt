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
                        // todo сюда видимо надо добавить вызов агрегации уровней при смены опции в domHeader
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