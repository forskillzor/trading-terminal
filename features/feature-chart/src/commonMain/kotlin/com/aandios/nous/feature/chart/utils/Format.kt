package com.aandios.nous.feature.chart.utils

import com.aandios.nous.core.ui.format.SymbolFormatter
import java.text.SimpleDateFormat
import java.util.Date

fun formatPrice(price: Float, formatter: SymbolFormatter = SymbolFormatter.DEFAULT): String {
    return formatter.formatPrice(price)
}

fun formatTime(timestamp: Long): String {
    val date = Date(timestamp)
    val formatter = SimpleDateFormat("HH:mm")
    return formatter.format(date)
}
