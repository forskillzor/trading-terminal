package com.aandios.nous.api.market.commands

import com.aandios.nous.api.market.model.OrderSide
import com.aandios.nous.api.market.model.OrderType

class TradeOffCommand(
    private val onResult: (CommandResult) -> Unit
) : TradingCommand {

    override suspend fun execute() {
        onResult(CommandResult.Success(OrderData(
            symbol = "SYSTEM",
            side = OrderSide.BUY, // dummy
            type = OrderType.MARKET, // dummy
            quantity = 0.0
        )))
    }

    override fun canExecute(): Boolean = true
    override fun getDescription(): String = "Toggle Trading ON/OFF"
}