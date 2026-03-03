package com.aandios.nous_platform.ui.terminalLayout

import kotlinx.coroutines.flow.MutableStateFlow

class TerminalStateViewModel {
    val selectedSymbol = MutableStateFlow<String>("BTCUSDT")
    val selectedTimeFrame = MutableStateFlow("1h")

    fun changeSymbol(symbol: String) {
        selectedSymbol.value = symbol
    }

    fun changeTimeFrame(timeframe: String) {
        selectedTimeFrame.value = timeframe
    }
}