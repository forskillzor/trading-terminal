package com.aandios.nous_platform.ui.dom

import com.aandios.nous_platform.data.api.binance.models.BestPrices
import com.aandios.nous_platform.domain.commands.*
import com.aandios.nous_platform.domain.entities.OrderBookData
import com.aandios.nous_platform.domain.repository.BestPricesRepository
import com.aandios.nous_platform.domain.repository.DomRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch

class DomViewModel(
    private val domRepository: DomRepository,
    private val bestPricesRepository: BestPricesRepository
) {
    private val viewModelScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var subscriptionJob: Job? = null

    private val _orderBook = MutableStateFlow<OrderBookData?>(null)
    val orderBook: StateFlow<OrderBookData?> = _orderBook.asStateFlow()

    private val _selectedPrice = MutableStateFlow<Double?>(null)
    val selectedPrice: StateFlow<Double?> = _selectedPrice.asStateFlow()

    private val _orderQuantity = MutableStateFlow("0.01")
    val orderQuantity: StateFlow<String> = _orderQuantity.asStateFlow()

    private val _isTradingEnabled = MutableStateFlow(true)
    val isTradingEnabled: StateFlow<Boolean> = _isTradingEnabled.asStateFlow()

    private val _lastCommandResult = MutableStateFlow<CommandResult?>(null)
    val lastCommandResult: StateFlow<CommandResult?> = _lastCommandResult.asStateFlow()

    private val _bestPrices = MutableStateFlow<BestPrices?>(null)
    val bestPrices: StateFlow<BestPrices?> = _bestPrices.asStateFlow()

    private var bestPricesJob: Job? = null

    fun subscribeToBestPrices(symbol: String) {
        bestPricesJob?.cancel()

        bestPricesJob = viewModelScope.launch {
            bestPricesRepository.getBestPrices(symbol)
                .catch { e ->
                    println("❌ BestPrices error: ${e.message}")
                }
                .collect { prices ->
                    _bestPrices.value = prices
                }
        }
    }

    fun subscribeToOrderBook(symbol: String) {
        println("📊 VM: Subscribing to $symbol")

        subscriptionJob?.cancel()

        subscriptionJob = viewModelScope.launch {
            domRepository.getOrderBook(symbol)
                .catch { e ->
                    println("❌ VM Error: ${e.message}")
                    e.printStackTrace()
                }
                .collect { data ->
                    _orderBook.value = data
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

    fun clear() {
        subscriptionJob?.cancel()
        viewModelScope.coroutineContext.cancelChildren()
    }
}