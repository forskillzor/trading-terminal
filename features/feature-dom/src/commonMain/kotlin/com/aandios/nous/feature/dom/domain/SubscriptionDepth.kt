package com.aandios.nous.feature.dom.domain

/**
 * Уровни глубины подписки на стакан заявок (order book levels).
 * Определяет количество уровней цен, которые будут отображаться и обновляться.
 */
enum class SubscriptionDepth(
    val levels: Int,
    val displayName: String
) {
    LEVELS_5(5, "5 уровней"),
    LEVELS_10(10, "10 уровней"),
    LEVELS_20(20, "20 уровней"),
    LEVELS_50(50, "50 уровней"),
    LEVELS_100(100, "100 уровней"),
    LEVELS_500(500, "500 уровней");

    companion object {
        /**
         * Возвращает значение по умолчанию (100 уровней).
         */
        fun default(): SubscriptionDepth = LEVELS_100

        /**
         * Находит SubscriptionDepth по количеству уровней.
         */
        fun fromLevels(levels: Int): SubscriptionDepth? {
            return values().firstOrNull { it.levels == levels }
        }

        /**
         * Возвращает список всех значений для использования в UI.
         */
        fun all(): List<SubscriptionDepth> = values().toList()
    }

    override fun toString(): String = displayName
}