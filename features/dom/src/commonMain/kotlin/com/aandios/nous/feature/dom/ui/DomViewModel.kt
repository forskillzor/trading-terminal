package com.aandios.nous.feature.dom.ui

import androidx.compose.runtime.mutableStateMapOf
import com.aandios.nous.api.market.commands.*
import com.aandios.nous.api.market.model.SymbolInfo
import com.aandios.nous.core.domain.repository.DomRepository
import com.aandios.nous.core.domain.repository.SymbolInfoRepository
import com.aandios.nous.feature.dom.domain.DomOptions
import com.aandios.nous.feature.dom.domain.TradingProvider
import com.aandios.nous.feature.dom.domain.TradingSymbol
import com.aandios.nous.api.market.model.orderbook.DomEvent
import com.aandios.nous.core.Disposable
import com.aandios.nous.feature.dom.domain.model.OrderIntent
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch

class DomViewModel(
    private val domRepository: DomRepository,
    private val symbolInfoRepository: SymbolInfoRepository? = null,
    private val coroutineDispatcher: CoroutineDispatcher? = null,
) : Disposable {
    private val dispatcher = coroutineDispatcher ?: Dispatchers.Default
    private val viewModelScope = CoroutineScope(dispatcher + SupervisorJob())
    private var subscriptionJob: Job? = null

    // ЕДИНЫЙ СТЕЙТ ВСЕХ НАСТРОЕК
    private val _domOptions = MutableStateFlow(DomOptions.default())
    val domOptions: StateFlow<DomOptions> = _domOptions.asStateFlow()

    private val _selectedPrice = MutableStateFlow<Double?>(null)
    val selectedPrice: StateFlow<Double?> = _selectedPrice.asStateFlow()

    private val _orderQuantity = MutableStateFlow("0.01")
    val orderQuantity: StateFlow<String> = _orderQuantity.asStateFlow()

    private val _isTradingEnabled = MutableStateFlow(true)
    val isTradingEnabled: StateFlow<Boolean> = _isTradingEnabled.asStateFlow()

    private val _lastCommandResult = MutableStateFlow<CommandResult?>(null)
    val lastCommandResult: StateFlow<CommandResult?> = _lastCommandResult.asStateFlow()

    // Символы, загруженные через symbolInfoRepository
    private val _loadedSymbols = MutableStateFlow<List<TradingSymbol>>(emptyList())
    val loadedSymbols: StateFlow<List<TradingSymbol>> = _loadedSymbols.asStateFlow()

    private val _symbolTickSize = MutableStateFlow<Double?>(null)
    val symbolTickSize: StateFlow<Double?> = _symbolTickSize.asStateFlow()

    // Инкрементальные данные DOM — SnapshotStateMap для in-place мутации
    // Compose отслеживает изменения по entry, без O(N) копии
    private val _incrementalBids = mutableStateMapOf<Double, Double>()
    val incrementalBids: Map<Double, Double> = _incrementalBids

    private val _incrementalAsks = mutableStateMapOf<Double, Double>()
    val incrementalAsks: Map<Double, Double> = _incrementalAsks

    // Best prices из BookTicker (отдельный стрим, не в картах)
    private val _incrementalBestBid = MutableStateFlow<Double?>(null)
    val incrementalBestBid: StateFlow<Double?> = _incrementalBestBid.asStateFlow()

    private val _incrementalBestAsk = MutableStateFlow<Double?>(null)
    val incrementalBestAsk: StateFlow<Double?> = _incrementalBestAsk.asStateFlow()

    private val _incrementalBestBidQuantity = MutableStateFlow<Double?>(null)
    val incrementalBestBidQuantity: StateFlow<Double?> = _incrementalBestBidQuantity.asStateFlow()

    private val _incrementalBestAskQuantity = MutableStateFlow<Double?>(null)
    val incrementalBestAskQuantity: StateFlow<Double?> = _incrementalBestAskQuantity.asStateFlow()

    override fun dispose() {
        subscriptionJob?.cancel()
        viewModelScope.cancel()
    }

    init {
        // Загружаем список всех символов через symbolInfoRepository (как в ChartViewModel)
        loadSymbols()

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
                oldOptions.depth != newOptions.depth

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
        subscriptionJob = viewModelScope.launch {
            subscribeToIncrementalDom(options)
        }
    }

    /**
     * Подписывается на инкрементальные события DOM и обновляет соответствующие StateFlow.
     */
    private suspend fun subscribeToIncrementalDom(options: DomOptions) {
        // Сбрасываем инкрементальные данные — in-place мутация, без аллокаций
        _incrementalBids.clear()
        _incrementalAsks.clear()
        _incrementalBestBid.value = null
        _incrementalBestAsk.value = null
        _incrementalBestBidQuantity.value = null
        _incrementalBestAskQuantity.value = null

        domRepository.subscribeToDomEvents(
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
     * Обрабатывает событие DomEvent и обновляет соответствующие коллекции.
     * SnapshotStateMap позволяет мутировать in-place без копирования всей карты.
     */
    private fun processDomEvent(event: DomEvent) {
        when (event) {
            is DomEvent.Snapshot -> {
                // Очищаем и загружаем из снапшота — in-place
                _incrementalBids.clear()
                _incrementalAsks.clear()

                event.snapshot.bids.forEach { (priceStr, qtyStr) ->
                    val price = priceStr.toDoubleOrNull()
                    val quantity = qtyStr.toDoubleOrNull()

                    if (price == null || quantity == null) {
                        println("⚠️ DomViewModel: Failed to parse snapshot bid data: price='$priceStr', quantity='$qtyStr'")
                        return@forEach
                    }

                    if (quantity > 0.0) _incrementalBids[price] = quantity
                }

                event.snapshot.asks.forEach { (priceStr, qtyStr) ->
                    val price = priceStr.toDoubleOrNull()
                    val quantity = qtyStr.toDoubleOrNull()

                    if (price == null || quantity == null) {
                        println("⚠️ DomViewModel: Failed to parse snapshot ask data: price='$priceStr', quantity='$qtyStr'")
                        return@forEach
                    }

                    if (quantity > 0.0) _incrementalAsks[price] = quantity
                }
            }

            is DomEvent.UpdateBid -> {
                if (event.quantity == 0.0) {
                    _incrementalBids.remove(event.price)
                } else {
                    _incrementalBids[event.price] = event.quantity
                }
            }

            is DomEvent.UpdateAsk -> {
                if (event.quantity == 0.0) {
                    _incrementalAsks.remove(event.price)
                } else {
                    _incrementalAsks[event.price] = event.quantity
                }
            }

            is DomEvent.BestPrices -> {
                _incrementalBestBid.value = event.bestBid
                _incrementalBestAsk.value = event.bestAsk
                _incrementalBestBidQuantity.value = event.bestBidQuantity
                _incrementalBestAskQuantity.value = event.bestAskQuantity
            }

            DomEvent.Reset -> {
                _incrementalBids.clear()
                _incrementalAsks.clear()
                _incrementalBestBid.value = null
                _incrementalBestAsk.value = null
                _incrementalBestBidQuantity.value = null
                _incrementalBestAskQuantity.value = null
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

    /**
     * Загружает все торговые символы через symbolInfoRepository.getAllSymbolsInfo()
     * и маппит их в TradingSymbol. Аналог ChartViewModel.loadSymbols().
     */
    private fun loadSymbols() {
        if (symbolInfoRepository == null) return

        viewModelScope.launch {
            try {
                val allSymbols = symbolInfoRepository?.getAllSymbolsInfo() ?: emptyList()
                val tradingSymbols = allSymbols
                    .filter { it.status == "TRADING" }
                    .map { TradingSymbol.fromSymbolInfo(it, _domOptions.value.provider) }
                    .sortedBy { it.symbol }
                if (tradingSymbols.isNotEmpty()) {
                    _loadedSymbols.value = tradingSymbols
                }
            } catch (e: Exception) {
                println("⚠️ Failed to load symbols from SymbolInfoRepository: ${e.message}")
            }
        }
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
