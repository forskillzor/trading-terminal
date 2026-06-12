package com.aandios.nous.provider.binance

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig

actual fun createBinanceHttpClient(configure: HttpClientConfig<*>.() -> Unit): HttpClient =
    HttpClient(configure)
