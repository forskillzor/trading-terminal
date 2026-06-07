package com.aandios.nous_platform.ui.dom

import com.aandios.nous.core.ui.format.SymbolFormatter

private val fmt = SymbolFormatter.DEFAULT

fun formatDomPrice(price: Double): String = fmt.formatPrice(price)
