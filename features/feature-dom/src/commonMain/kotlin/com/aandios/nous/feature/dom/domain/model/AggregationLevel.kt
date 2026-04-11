package com.aandios.nous.feature.dom.domain.model

/**
 * Уровень агрегации цен в стакане заявок (DOM).
 * Определяет размер тика, в пределах которого заявки суммируются.
 *
 * Например, при уровне 0.1 все заявки с ценами в интервале [n*0.1, (n+1)*0.1)
 * будут объединены в один уровень с агрегированной ценой n*0.1.
 */
enum class AggregationLevel(val tickSize: Double) {
    /** Агрегация по тику 0.1 (например, 123.45 → 123.4) */
    TICK_0_1(0.1),

    /** Агрегация по тику 1.0 (например, 123.45 → 123.0) */
    TICK_1_0(1.0),

    /** Агрегация по тику 10.0 (например, 123.45 → 120.0) */
    TICK_10_0(10.0);

    companion object {
        /**
         * Возвращает список всех уровней агрегации в порядке увеличения тика.
         */
        fun all(): List<AggregationLevel> = listOf(TICK_0_1, TICK_1_0, TICK_10_0)

        /**
         * Возвращает уровень агрегации по его строковому представлению (например, "0.1").
         * @throws IllegalArgumentException если строка не соответствует ни одному уровню
         */
        fun fromString(value: String): AggregationLevel = when (value) {
            "0.1" -> TICK_0_1
            "1.0" -> TICK_1_0
            "10" -> TICK_10_0
            else -> throw IllegalArgumentException("Unknown aggregation level: $value")
        }
    }

    /**
     * Возвращает строковое представление уровня агрегации для отображения в UI.
     */
    fun displayName(): String = when (this) {
        TICK_0_1 -> "0.1"
        TICK_1_0 -> "1.0"
        TICK_10_0 -> "10"
    }

    /**
     * Округляет цену вниз до ближайшего тика агрегации.
     * Например, для цены 123.45 и уровня 0.1 вернёт 123.4.
     */
    fun roundDown(price: Double): Double {
        if (tickSize <= 0.0) return price
        return (price / tickSize).toInt() * tickSize
    }

    /**
     * Вычисляет ключ агрегации для заданной цены (строковое представление).
     * Возвращает цену, округлённую вниз до тика, в виде строки с фиксированным количеством знаков после запятой.
     */
    fun aggregationKey(price: String): String {
        val priceDouble = price.toDoubleOrNull() ?: return price
        val rounded = roundDown(priceDouble)
        // Форматируем без лишних нулей, но с достаточной точностью
        return if (rounded == rounded.toInt().toDouble()) {
            rounded.toInt().toString()
        } else {
            // Убираем trailing zeros
            rounded.toString().trimEnd('0').trimEnd('.')
        }
    }
}