package com.aandios.nous.feature.dom.domain

/**
 * Время агрегации данных для отображения в DOM.
 * Определяет интервал обновления и агрегации данных.
 */
enum class AggregationTime(
    val displayName: String,
    val milliseconds: Long
) {
    REAL_TIME("Real-time", 0L),
    ONE_SECOND("1s", 1000L),
    FIVE_SECONDS("5s", 5000L),
    TEN_SECONDS("10s", 10000L),
    THIRTY_SECONDS("30s", 30000L),
    ONE_MINUTE("1m", 60000L),
    FIVE_MINUTES("5m", 300000L);

    companion object {
        /**
         * Значение по умолчанию (Real-time).
         */
        fun default(): AggregationTime = REAL_TIME

        /**
         * Находит AggregationTime по значению в миллисекундах.
         */
        fun fromMilliseconds(ms: Long): AggregationTime {
            return values().find { it.milliseconds == ms } ?: REAL_TIME
        }
    }

    override fun toString(): String = displayName
}