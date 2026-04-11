package com.aandios.nous.feature.dom.domain

import com.aandios.nous.api.market.model.BookTicker
import com.aandios.nous.api.market.model.orderbook.OrderBook
import com.aandios.nous.api.market.model.orderbook.OrderBookLevel
import com.aandios.nous.feature.dom.domain.model.AggregationLevel

/**
 * Унифицированное представление стакана котировок, где каждый уровень содержит
 * как bid, так и ask объемы для данной цены.
 *
 * Эта модель предназначена для передачи в UI-слой, чтобы избежать преобразований
 * на стороне UI и централизовать бизнес-логику в репозитории.
 */
data class UnifiedOrderBook(
    val symbol: String,
    val levels: List<OrderBookLevel>,
    val timestamp: Long,
    val bestBid: Double?,
    val bestAsk: Double?,
    val spread: Double?,
    val spreadPercent: Double?
) {
    companion object {
        fun fromOrderBook(
            orderBook: OrderBook,
            bookTicker: BookTicker?
        ): UnifiedOrderBook {
            val bids = orderBook.bids
            val asks = orderBook.asks
            
            // Создаем карту цен для объединения
            val priceMap = mutableMapOf<String, OrderBookLevel>()
            
            // Добавляем bids
            bids.forEach { level ->
                priceMap[level.price] = level.copy(
                    bidQty = level.quantity,
                    askQty = ""
                )
            }
            
            // Добавляем asks, объединяя с существующими ценами
            asks.forEach { level ->
                val existing = priceMap[level.price]
                if (existing != null) {
                    // Цена есть в bids, обновляем askQty
                    priceMap[level.price] = existing.copy(
                        askQty = level.quantity
                    )
                } else {
                    // Новая цена только в asks
                    priceMap[level.price] = level.copy(
                        bidQty = "",
                        askQty = level.quantity
                    )
                }
            }
            
            // Сортируем по цене в порядке убывания (как в стакане)
            val sortedLevels = priceMap.values.sortedByDescending { 
                it.price.toDoubleOrNull() ?: 0.0 
            }
            
            val bestBid = bids.firstOrNull()?.price?.toDoubleOrNull()
            val bestAsk = asks.firstOrNull()?.price?.toDoubleOrNull()
            val spread = if (bestBid != null && bestAsk != null) bestAsk - bestBid else null
            val spreadPercent = if (bestBid != null && spread != null) (spread / bestBid) * 100 else null
            
            return UnifiedOrderBook(
                symbol = orderBook.symbol,
                levels = sortedLevels,
                timestamp = orderBook.timestamp,
                bestBid = bestBid,
                bestAsk = bestAsk,
                spread = spread,
                spreadPercent = spreadPercent
            )
        }
    }
    
    /**
     * Возвращает максимальный объем среди всех bid и ask для масштабирования визуализации.
     */
    fun maxVolume(): Double {
        return levels.maxOfOrNull { level ->
            maxOf(
                level.bidQty.toDoubleOrNull() ?: 0.0,
                level.askQty.toDoubleOrNull() ?: 0.0
            )
        } ?: 1.0
    }
    
    /**
     * Создает новый UnifiedOrderBook с агрегированными уровнями по заданному тику.
     * @param aggregationLevel уровень агрегации (0.1, 1.0, 10.0)
     * @return новый UnifiedOrderBook с агрегированными уровнями
     */
    fun aggregate(aggregationLevel: AggregationLevel): UnifiedOrderBook {
        val aggregatedLevels = DomAggregator.aggregateUnifiedLevels(levels, aggregationLevel)
        
        // Агрегируем лучшие цены с учетом уровня агрегации
        val aggregatedBestBid = bestBid?.let { aggregationLevel.roundDown(it) }
        val aggregatedBestAsk = bestAsk?.let { aggregationLevel.roundDown(it) }
        val aggregatedSpread = if (aggregatedBestBid != null && aggregatedBestAsk != null) 
            aggregatedBestAsk - aggregatedBestBid else null
        val aggregatedSpreadPercent = if (aggregatedBestBid != null && aggregatedSpread != null)
            (aggregatedSpread / aggregatedBestBid) * 100 else null
        
        return copy(
            levels = aggregatedLevels,
            bestBid = aggregatedBestBid,
            bestAsk = aggregatedBestAsk,
            spread = aggregatedSpread,
            spreadPercent = aggregatedSpreadPercent
        )
    }
}