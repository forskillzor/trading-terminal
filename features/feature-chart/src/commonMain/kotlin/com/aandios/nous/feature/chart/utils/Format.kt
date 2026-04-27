package com.aandios.nous.feature.chart.utils

import java.text.SimpleDateFormat
import java.util.Date

fun formatPrice(price: Float): String {
    return when {
        price >= 1000 -> String.format("%.1f", price)
        price >= 100 -> String.format("%.2f", price)
        price >= 10 -> String.format("%.3f", price)
        price >= 1 -> String.format("%.4f", price)
        else -> String.format("%.6f", price)
    }
}

fun formatTime(timestamp: Long): String {
    val date = Date(timestamp)
    val formatter = SimpleDateFormat("HH:mm")
    return formatter.format(date)
}
