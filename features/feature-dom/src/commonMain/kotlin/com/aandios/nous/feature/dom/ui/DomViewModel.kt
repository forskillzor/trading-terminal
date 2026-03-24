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
import com.aandios.nous.feature.dom.data.repository.subscribeToUnifiedOrderBook
import com.aandios.nous.feature.dom.domain.AggregationLevel
import com.aandios.nous.feature.dom.domain.SubscriptionDepth
import com.aandios.nous.feature.dom.domain.UnifiedOrderBook
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import java.util.concurrent.Executors

class DomViewModel(
    private val domRepository: DomRepository,
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

    private var bookTickerJob: Job? = null
    private var unifiedSubscriptionJob: Job? = null

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

    fun updateSubscriptionDepth(depth: SubscriptionDepth) {
        if (_subscriptionDepth.value != depth) {
            println("📊 VM: Subscription depth changed to ${depth.displayName}")
            _subscriptionDepth.value = depth
        }
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