package com.aandios.nous.core.ui.format

import kotlin.math.abs
import kotlin.math.log10
import kotlin.math.max

/**
 * Унифицированное форматирование цен и объёмов с учётом tickSize и minQty инструмента.
 *
 * @param tickSize минимальный шаг цены (из SymbolInfo, например BTCUSDT=0.01, ETHUSDT=0.1)
 * @param minQty минимальный объём сделки (из SymbolInfo, например BTCUSDT=0.001)
 */
class SymbolFormatter(
    val tickSize: Double = 0.01,
    val minQty: Double = 0.001
) {
    /** Количество знаков после запятой для цен: 0.01→2, 0.1→1, 1.0→0 */
    val priceDecimals: Int = if (tickSize <= 0.0) 2 else maxOf(0, -log10(tickSize).toInt())

    /** Количество знаков после запятой для объёмов: 0.001→3, 0.00001→5 */
    val volumeDecimals: Int = if (minQty <= 0.0) 3 else maxOf(0, max(0, -log10(minQty).toInt()))

    fun formatPrice(price: Double): String {
        val decimals = when {
            price >= 100_000 -> maxOf(1, priceDecimals - 2)
            price >= 10_000 -> maxOf(1, priceDecimals - 1)
            price >= 1_000 -> priceDecimals
            price >= 100 -> priceDecimals + 1
            price >= 10 -> priceDecimals + 2
            price >= 1 -> priceDecimals + 2
            else -> priceDecimals + 3
        }
        val fmt = "%.${decimals}f"
        return String.format(fmt, price)
    }

    fun formatPrice(price: Float): String = formatPrice(price.toDouble())

    fun formatVolume(volume: Double): String {
        val v = abs(volume)
        return when {
            v >= 1_000_000 -> String.format("%.${maxOf(1, volumeDecimals - 2)}fM", volume / 1_000_000)
            v >= 1_000 -> String.format("%.${maxOf(1, volumeDecimals - 1)}fK", volume / 1_000)
            v >= 100 -> String.format("%.${coerceMaxDecimals(0)}f", volume)
            v >= 10 -> String.format("%.${coerceMaxDecimals(1)}f", volume)
            v >= 1 -> String.format("%.${coerceMaxDecimals(volumeDecimals - 1)}f", volume)
            else -> String.format("%.${volumeDecimals}f", volume)
        }
    }

    fun formatVolume(volume: Float): String = formatVolume(volume.toDouble())

    private fun coerceMaxDecimals(d: Int) = d.coerceAtMost(volumeDecimals).coerceAtLeast(0)

    companion object {
        /** Default formatter for BTCUSDT (tickSize=0.01, minQty=0.001) */
        val DEFAULT = SymbolFormatter()
    }
}
