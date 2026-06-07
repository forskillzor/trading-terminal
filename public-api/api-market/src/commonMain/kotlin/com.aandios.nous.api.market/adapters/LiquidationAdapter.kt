package com.aandios.nous.api.market.adapters

import com.aandios.nous.api.market.model.liquidation.LiquidationOrder
import kotlinx.coroutines.flow.Flow

interface LiquidationAdapter : MarketAdapter {
    /**
     * Subscribe to real-time liquidation orders for a symbol.
     * Binance stream: {symbol}@forceOrder
     */
    fun subscribeToLiquidations(symbol: String): Flow<LiquidationOrder>
}
