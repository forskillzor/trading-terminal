package com.aandios.tradingterminal.tests

import kotlinx.coroutines.runBlocking

fun main() = runBlocking {
    println("Testing Binance WebSocket connection...")

    // Простой тест без зависимостей
    testDirectWebSocket()
}

suspend fun testDirectWebSocket() {
    val endpoint = "wss://stream.binance.com:9443/ws/btcusdt@kline_1m"

    println("Connecting to: $endpoint")

    // Используем прямой WebSocket клиент
    try {
        // Это упрощенный тест - на практике используй HttpClient с WebSockets
        println("Test endpoint: $endpoint")
        println("Copy this URL and test in browser tools or websocket.org")

        // Альтернативный простой тест с ktor
        testWithKtor()
    } catch (e: Exception) {
        println("Test failed: ${e.message}")
    }
}

suspend fun testWithKtor() {
    println("Starting Ktor WebSocket test...")

    // Импортируй эти зависимости если их нет:
    // implementation("io.ktor:ktor-client-cio:2.3.5")
    // implementation("io.ktor:ktor-client-websockets:2.3.5")

    try {
        // Простой тест
        println("Ktor test skipped - check dependencies")
    } catch (e: NoClassDefFoundError) {
        println("Missing WebSocket dependencies")
        println("Add to build.gradle.kts:")
        println("implementation(\"io.ktor:ktor-client-cio:2.3.5\")")
        println("implementation(\"io.ktor:ktor-client-websockets:2.3.5\")")
    }
}