package com.aandios.nous.api.market

sealed class ValidationResult {
    object Success : ValidationResult()
    data class Error(
        val message: String,
        val details: Map<String, String> = emptyMap()
    ) : ValidationResult()
}