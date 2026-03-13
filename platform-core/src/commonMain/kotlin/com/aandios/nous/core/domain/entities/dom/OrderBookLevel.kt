package com.aandios.nous.core.domain.entities.dom

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
data class OrderBookLevel(
    val price: String,
    val quantity: String,
    val total: String = "0",
    @Transient
    val stableId: String = "${price}-${System.nanoTime()}" // Уникальный ID
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is OrderBookLevel) return false
        return price == other.price // Сравниваем только по цене
    }

    override fun hashCode(): Int = price.hashCode()
}
