package com.aandios.nous.feature.dom.ui

import com.aandios.nous.core.ui.format.SymbolFormatter

private val symFmt = SymbolFormatter.DEFAULT

fun formatDomPrice(price: Double): String = symFmt.formatPrice(price)
