package com.aandios.nous.feature.chart.indicator

import com.aandios.nous.api.market.adapters.LiquidationAdapter
import com.aandios.nous.api.market.model.liquidation.LiquidationOrder
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

data class LiquidationState(
    val orders: List<LiquidationOrder> = emptyList(),
    val connected: Boolean = false,
    val error: String? = null
)

class LiquidationViewModel(
    private val liquidationAdapter: LiquidationAdapter?
) {
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var subscriptionJob: Job? = null

    private val _state = MutableStateFlow(LiquidationState())
    val state: StateFlow<LiquidationState> = _state.asStateFlow()

    fun subscribe(symbol: String) {
        unsubscribe()
        if (liquidationAdapter == null) {
            _state.value = _state.value.copy(error = "Liquidation adapter not available")
            return
        }

        _state.value = LiquidationState(orders = emptyList(), connected = true)

        subscriptionJob = scope.launch {
            try {
                liquidationAdapter.subscribeToLiquidations(symbol)
                    .catch { e ->
                        _state.value = _state.value.copy(connected = false, error = e.message)
                    }
                    .collect { order ->
                        val current = _state.value.orders.toMutableList()
                        current.add(order)
                        // Keep last 1000 orders
                        if (current.size > 1000) {
                            current.removeAt(0)
                        }
                        _state.value = _state.value.copy(orders = current)
                    }
            } catch (e: CancellationException) {
                // normal stop
            } catch (e: Exception) {
                _state.value = _state.value.copy(connected = false, error = e.message)
            }
        }
    }

    fun unsubscribe() {
        subscriptionJob?.cancel()
        subscriptionJob = null
        _state.value = LiquidationState()
    }

    fun clear() {
        unsubscribe()
        scope.cancel()
    }
}
