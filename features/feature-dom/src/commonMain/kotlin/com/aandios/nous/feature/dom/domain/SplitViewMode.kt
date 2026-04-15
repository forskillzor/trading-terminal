package com.aandios.nous.feature.dom.domain

/**
 * Режим отображения для split mode.
 * Определяет какие столбцы показывать в split режиме.
 */
enum class SplitViewMode(val displayName: String) {
    BID_ASK("Bid & Ask"),  // оба столбца + spread
    BID_ONLY("Bid Only"),  // только bid столбец
    ASK_ONLY("Ask Only")   // только ask столбец
}