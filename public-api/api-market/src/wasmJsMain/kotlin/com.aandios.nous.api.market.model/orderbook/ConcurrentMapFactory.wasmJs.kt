package com.aandios.nous.api.market.model.orderbook

actual fun <K, V> concurrentMap(): MutableMap<K, V> = mutableMapOf()
