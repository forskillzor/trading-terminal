package com.aandios.nous.api.market

data class ProviderConfig(
    val apiKey: String? = null,
    val secretKey: String? = null,
    val isTestnet: Boolean = false,
    val customSettings: Map<String, String> = emptyMap()
)
