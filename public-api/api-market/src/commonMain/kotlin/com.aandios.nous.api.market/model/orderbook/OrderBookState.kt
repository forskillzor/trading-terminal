package com.aandios.nous.api.market.model.orderbook

import java.util.concurrent.ConcurrentHashMap

class OrderBookState {
    val bids = ConcurrentHashMap<String, String>()  // цена -> объем
    val asks = ConcurrentHashMap<String, String>()  // цена -> объем
    var lastUpdateId: Long = 0

    fun updateFromSnapshot(snapshot: DepthSnapshot) {
        snapshot.bids.forEach { (price, qty) ->
            if (qty.toDoubleOrNull() != 0.0) bids[price] = qty
        }
        snapshot.asks.forEach { (price, qty) ->
            if (qty.toDoubleOrNull() != 0.0) asks[price] = qty
        }
        lastUpdateId = snapshot.lastUpdateId
    }

    fun applyUpdate(update: DepthUpdate) {
        // Обновляем bids
        update.bids.forEach { (price, qty) ->
            if (qty.toDoubleOrNull() == 0.0) {
                bids.remove(price)      // удаляем уровень
            } else {
                bids[price] = qty       // обновляем/добавляем
            }
        }

        // Обновляем asks
        update.asks.forEach { (price, qty) ->
            if (qty.toDoubleOrNull() == 0.0) {
                asks.remove(price)      // удаляем уровень
            } else {
                asks[price] = qty       // обновляем/добавляем
            }
        }

        lastUpdateId = update.finalUpdateId
    }

    /**
     * Получить bids, обрезанные по лучшей цене
     */
    fun getBidsUpToBestBid(bestBid: Double): List<OrderBookLevel> {
        return bids
            .filter { it.key.toDouble() <= bestBid }  // только цены <= bestBid
            .map { OrderBookLevel(it.key, it.value) }
            .sortedByDescending { it.price.toDouble() }
    }

    /**
     * Получить asks, обрезанные по лучшей цене
     */
    fun getAsksFromBestAsk(bestAsk: Double): List<OrderBookLevel> {
        return asks
            .filter { it.key.toDouble() >= bestAsk }  // только цены >= bestAsk
            .map { OrderBookLevel(it.key, it.value) }
            .sortedBy { it.price.toDouble() }
    }
}