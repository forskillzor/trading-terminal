package com.aandios.nous.core.workspace

import kotlinx.serialization.Serializable

@Serializable
data class ProviderRef(
    val id: String,
    val name: String,
    val isTestnet: Boolean = false,
    val symbols: List<String> = emptyList()
)
