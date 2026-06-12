package com.aandios.nous.provider.binance

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.cio.CIO

actual fun createBinanceHttpClient(configure: HttpClientConfig<*>.() -> Unit): HttpClient =
    HttpClient(CIO, configure)
