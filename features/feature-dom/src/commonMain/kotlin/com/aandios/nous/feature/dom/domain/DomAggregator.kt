package com.aandios.nous.feature.dom.domain

import com.aandios.nous.api.market.model.orderbook.OrderBookLevel
import com.aandios.nous.feature.dom.domain.model.AggregationLevel

/**
 * Сервис агрегации уровней стакана заявок (DOM) по заданному тику.
 * Объединяет заявки, попадающие в один агрегированный уровень, суммируя их объёмы.
 */
object DomAggregator {

    /**
     * Агрегирует список уровней стакана по заданному уровню агрегации.
     *
     * @param levels исходный список уровней (может быть как bid, так и ask)
     * @param aggregationLevel уровень агрегации (тик)
     * @return новый список агрегированных уровней, отсортированный в том же порядке, что и исходный
     *         (по убыванию цены для bids, по возрастанию для asks)
     */
    fun aggregateLevels(
        levels: List<OrderBookLevel>,
        aggregationLevel: AggregationLevel
    ): List<OrderBookLevel> {
        if (levels.isEmpty() || aggregationLevel.tickSize <= 0.0) {
            return levels
        }

        // Группируем по ключу агрегации (цена, округлённая вниз до тика)
        val grouped = levels.groupBy { level ->
            aggregationLevel.aggregationKey(level.price)
        }

        // Для каждой группы создаём агрегированный уровень
        return grouped.map { (aggregatedPrice, group) ->
            // Суммируем quantity
            val totalQuantity = group.sumOf { level ->
                level.quantity.toDoubleOrNull() ?: 0.0
            }

            // Суммируем bidQty и askQty, если они присутствуют (для унифицированного стакана)
            val totalBidQty = group.sumOf { level ->
                level.bidQty.takeIf { it.isNotBlank() }?.toDoubleOrNull() ?: 0.0
            }
            val totalAskQty = group.sumOf { level ->
                level.askQty.takeIf { it.isNotBlank() }?.toDoubleOrNull() ?: 0.0
            }

            // Берём первый уровень как образец для остальных полей (price используем агрегированную)
            val sample = group.first()
            OrderBookLevel(
                price = aggregatedPrice,
                quantity = totalQuantity.toString(),
                total = "", // total будет пересчитан позже в calculateTotals
                bidQty = if (totalBidQty > 0.0) totalBidQty.toString() else "",
                askQty = if (totalAskQty > 0.0) totalAskQty.toString() else ""
            )
        }
        // Сохраняем исходный порядок: для этого сортируем по цене как в исходном списке.
        // Поскольку ключ агрегации может изменить порядок, нужно отсортировать по агрегированной цене
        // в том же направлении, что и исходный список.
        .sortedBy { it.price.toDoubleOrNull() ?: 0.0 }
        // Направление сортировки определяется вызывающим кодом (bids descending, asks ascending).
        // Здесь мы просто возвращаем список, который потом будет отсортирован соответствующим образом.
    }

    /**
     * Агрегирует полный стакан (bids и asks) по заданному уровню агрегации.
     *
     * @param bids список уровней покупок (должны быть отсортированы по убыванию цены)
     * @param asks список уровней продаж (должны быть отсортированы по возрастанию цены)
     * @param aggregationLevel уровень агрегации
     * @return пара (агрегированные bids, агрегированные asks)
     */
    fun aggregateOrderBook(
        bids: List<OrderBookLevel>,
        asks: List<OrderBookLevel>,
        aggregationLevel: AggregationLevel
    ): Pair<List<OrderBookLevel>, List<OrderBookLevel>> {
        val aggregatedBids = aggregateLevels(bids, aggregationLevel)
            .sortedByDescending { it.price.toDoubleOrNull() ?: 0.0 }
        val aggregatedAsks = aggregateLevels(asks, aggregationLevel)
            .sortedBy { it.price.toDoubleOrNull() ?: 0.0 }
        return aggregatedBids to aggregatedAsks
    }

    /**
     * Агрегирует унифицированный стакан (UnifiedOrderBook) по заданному уровню агрегации.
     * Объединяет уровни, которые имеют как bidQty, так и askQty.
     *
     * @param unifiedLevels список унифицированных уровней
     * @param aggregationLevel уровень агрегации
     * @return агрегированный список унифицированных уровней, отсортированный по убыванию цены
     */
    fun aggregateUnifiedLevels(
        unifiedLevels: List<OrderBookLevel>,
        aggregationLevel: AggregationLevel
    ): List<OrderBookLevel> {
        if (unifiedLevels.isEmpty()) return emptyList()

        // Группируем по ключу агрегации
        val grouped = unifiedLevels.groupBy { level ->
            aggregationLevel.aggregationKey(level.price)
        }

        return grouped.map { (aggregatedPrice, group) ->
            // Суммируем bidQty и askQty
            val totalBidQty = group.sumOf { level ->
                level.bidQty.takeIf { it.isNotBlank() }?.toDoubleOrNull() ?: 0.0
            }
            val totalAskQty = group.sumOf { level ->
                level.askQty.takeIf { it.isNotBlank() }?.toDoubleOrNull() ?: 0.0
            }

            val sample = group.first()
            OrderBookLevel(
                price = aggregatedPrice,
                quantity = "", // quantity не используется в унифицированном стакане
                total = "",
                bidQty = totalBidQty.toString(),
                askQty = totalAskQty.toString()
            )
        }.sortedByDescending { it.price.toDoubleOrNull() ?: 0.0 }
    }
}