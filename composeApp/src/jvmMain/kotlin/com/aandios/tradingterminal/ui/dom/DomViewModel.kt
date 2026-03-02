package com.aandios.tradingterminal.ui.dom

import androidx.compose.ui.geometry.Offset
import com.aandios.tradingterminal.domain.entities.OrderBookData
import com.aandios.tradingterminal.domain.entities.OrderSide
import com.aandios.tradingterminal.domain.repository.DomRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

class DomViewModel(
    private val domRepository: DomRepository
) {
    private val viewModelScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var subscriptionJob: Job? = null

    // Простой MutableStateFlow - никаких сложных трансформаций!
    private val _orderBook = MutableStateFlow<OrderBookData?>(null)
    val orderBook: StateFlow<OrderBookData?> = _orderBook.asStateFlow()

    private val _selectedPrice = MutableStateFlow<Double?>(null)
    val selectedPrice: StateFlow<Double?> = _selectedPrice.asStateFlow()

    private val _mousePosition = MutableStateFlow<Offset?>(null)
    val mousePosition: StateFlow<Offset?> = _mousePosition.asStateFlow()

    private val _orderQuantity = MutableStateFlow("0.01")
    val orderQuantity: StateFlow<String> = _orderQuantity.asStateFlow()

    fun subscribeToOrderBook(symbol: String) {
        println("📊 DomViewModel: Subscribing to $symbol")

        subscriptionJob?.cancel()

        subscriptionJob = viewModelScope.launch {
            domRepository.getOrderBook(symbol)
                .catch { e ->
                    e.printStackTrace()
                }
                .collect { data ->
                    if (data.bids.isNotEmpty()) {
                        println("from dom viewmodel Best bid: ${data.bids.first().price}")
                    }
                    if (data.asks.isNotEmpty()) {
                        println("from dom viewmodel Best ask: ${data.asks.first().price}")
                    }

                    _orderBook.value = data
                }
        }
    }

    fun selectPrice(price: Double?) {
        if (_selectedPrice.value != price) {
            println("💰 Price selected: $price")
            _selectedPrice.value = price
        }
    }

    fun updateMousePosition(position: Offset?) {
        _mousePosition.value = position
    }

    fun updateOrderQuantity(quantity: String) {
        _orderQuantity.value = quantity
    }

    fun placeOrder(side: OrderSide) {
        val price = _selectedPrice.value
        val quantity = _orderQuantity.value.toDoubleOrNull()

        if (price != null && quantity != null && quantity > 0) {
            println("📝 Placing $side order: $quantity @ $price")
            // Здесь будет логика размещения ордера
        } else {
            println("❌ Cannot place order: invalid price or quantity")
        }
    }

    fun clear() {
        subscriptionJob?.cancel()
        viewModelScope.coroutineContext.cancelChildren()
        viewModelScope.cancel()
    }
}