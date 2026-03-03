package com.aandios.tradingterminal.domain.commands

class TradeOffCommand(
    private val onResult: (CommandResult) -> Unit
) : TradingCommand {

    override suspend fun execute() {
        onResult(CommandResult.Success(OrderData(
            symbol = "SYSTEM",
            side = com.aandios.tradingterminal.domain.entities.OrderSide.BUY, // dummy
            type = com.aandios.tradingterminal.domain.entities.OrderType.MARKET, // dummy
            quantity = 0.0
        )))
    }

    override fun canExecute(): Boolean = true
    override fun getDescription(): String = "Toggle Trading ON/OFF"
}