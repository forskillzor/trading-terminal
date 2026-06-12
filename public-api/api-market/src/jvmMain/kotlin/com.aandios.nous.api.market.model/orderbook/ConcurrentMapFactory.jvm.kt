package com.aandios.nous.api.market.model.orderbook

import java.util.concurrent.ConcurrentHashMap

actual fun <K, V> concurrentMap(): MutableMap<K, V> = ConcurrentHashMap()
