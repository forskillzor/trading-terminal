package com.aandios.tradingterminal.domain.commands

import com.aandios.tradingterminal.domain.entities.OrderSide
import com.aandios.tradingterminal.domain.entities.OrderType

// Базовый интерфейс команды
interface TradingCommand {
    suspend fun execute()
    fun canExecute(): Boolean
    fun getDescription(): String
}

// Данные ордера
data class OrderData(
    val symbol: String,
    val side: OrderSide,
    val type: OrderType,
    val price: Double? = null,  // null для market ордеров
    val quantity: Double,
    val timestamp: Long = System.currentTimeMillis()
)

// Результат выполнения
sealed class CommandResult {
    data class Success(val orderData: OrderData) : CommandResult()
    data class Error(val message: String) : CommandResult()
    object TradingDisabled : CommandResult()
}