package com.aandios.nous.core.storage

interface StateStore {
    suspend fun getString(key: String): String?
    suspend fun putString(key: String, value: String)
}
