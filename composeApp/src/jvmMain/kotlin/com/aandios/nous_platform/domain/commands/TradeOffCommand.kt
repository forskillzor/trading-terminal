package com.aandios.nous_platform.domain.commands

class TradeOffCommand(
    private val onResult: (CommandResult) -> Unit
) : TradingCommand {

    override suspend fun execute() {
        onResult(CommandResult.Success(OrderData(
            symbol = "SYSTEM",
            side = com.aandios.nous_platform.domain.entities.OrderSide.BUY, // dummy
            type = com.aandios.nous_platform.domain.entities.OrderType.MARKET, // dummy
            quantity = 0.0
        )))
    }

    override fun canExecute(): Boolean = true
    override fun getDescription(): String = "Toggle Trading ON/OFF"
}