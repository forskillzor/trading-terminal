package com.aandios.nous.api.market

import io.ktor.client.HttpClient

interface NetworkManager {
    val httpClient: HttpClient
}
