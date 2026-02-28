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

    // Внутренний MutableStateFlow для хранения данных
    private val _orderBook = MutableStateFlow<OrderBookData?>(null)

    // Внешний StateFlow с фильтрацией дубликатов
    val orderBook: StateFlow<OrderBookData?> = _orderBook
        .asStateFlow()
        .filterNotNull() // Добавляем фильтр, если нужно отсеять null
        .distinctUntilChanged { old, new ->
            // Сравниваем только если изменились цены или объемы
            old.bids.firstOrNull()?.price == new.bids.firstOrNull()?.price &&
                    old.asks.firstOrNull()?.price == new.asks.firstOrNull()?.price
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = null
        )

    private val _selectedPrice = MutableStateFlow<Double?>(null)
    val selectedPrice: StateFlow<Double?> = _selectedPrice.asStateFlow()

    private val _mousePosition = MutableStateFlow<Offset?>(null)
    val mousePosition: StateFlow<Offset?> = _mousePosition.asStateFlow()

    private val _orderQuantity = MutableStateFlow("0.01")
    val orderQuantity: StateFlow<String> = _orderQuantity.asStateFlow()

    fun subscribeToOrderBook(symbol: String) {
        subscriptionJob?.cancel()

        subscriptionJob = viewModelScope.launch {
            domRepository.getOrderBook(symbol)
                .catch { e ->
                    println("❌ DOM subscription error: ${e.message}")
                }
                .collect { data ->
                    // Обновляем внутренний MutableStateFlow
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
        }
    }

    fun clear() {
        viewModelScope.coroutineContext.cancelChildren()
        viewModelScope.cancel()
    }
}