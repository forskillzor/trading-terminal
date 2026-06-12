package com.aandios.nous.bidasker.web

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.math.*
import kotlinx.datetime.*

fun main() {
    FootprintApp().start()
}

external fun setTimeout(callback: () -> Unit, delay: Int): Int

external interface MessageEventLike {
    val data: JsAny?
}

external interface Window {
    val innerWidth: Int
    val innerHeight: Int
    val document: Document
    fun addEventListener(type: String, handler: () -> Unit)
}

external interface Document {
    fun createElement(tag: String): Element
    val body: Body?
}

external interface Element {
    var id: String
    var style: Style
    val width: Int
    val height: Int
    fun setAttribute(name: String, value: String)
    fun appendChild(child: Element): Element
    fun getContext(contextId: String): CanvasContext?
    fun addEventListener(type: String, handler: (MessageEventLike) -> Unit)
}

external interface Body {
    fun appendChild(child: Element): Element
}

external interface Style {
    var position: String
    var top: String
    var left: String
    var width: String
    var height: String
    var background: String
}

external interface CanvasContext {
    fun clearRect(x: Double, y: Double, w: Double, h: Double)
    fun fillRect(x: Double, y: Double, w: Double, h: Double)
    fun strokeRect(x: Double, y: Double, w: Double, h: Double)
    fun fillText(text: String, x: Double, y: Double)
    fun strokeText(text: String, x: Double, y: Double)
    var fillStyle: String
    var strokeStyle: String
    var font: String
    var textAlign: String
    var lineWidth: Double
}

external val window: Window

class FootprintApp {
    private val httpClient = HttpClient {
        install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true; isLenient = true }) }
    }
    private val baseUrl = "http://95.81.99.28:8085"
    private var canvas: Element? = null
    private var ctx: CanvasContext? = null
    private var symbol = "BTCUSDT"
    private var timeframe = "5m"
    private var candles: List<FootprintCandleData> = emptyList()
    private var loading = true

    fun start() {
        val c = window.document.createElement("canvas")
        c.id = "chart"
        c.setAttribute("width", "${window.innerWidth}")
        c.setAttribute("height", "${window.innerHeight}")
        c.style.position = "fixed"
        c.style.top = "0"
        c.style.left = "0"
        c.style.width = "100%"
        c.style.height = "100%"
        c.style.background = "#0A0A0A"
        window.document.body?.appendChild(c)
        canvas = c
        ctx = c.getContext("2d")

        window.addEventListener("resize") {
            c.setAttribute("width", "${window.innerWidth}")
            c.setAttribute("height", "${window.innerHeight}")
        }

        val scope = MainScope()
        scope.launch {
            loadInstruments()
            refreshLoop(scope)
        }
    }

    private suspend fun refreshLoop(scope: CoroutineScope) {
        while (scope.isActive) {
            loadCandles()
            render()
            delay(10_000)
        }
    }

    private suspend fun loadInstruments() {
        try {
            val response: String = httpClient.get("$baseUrl/api/instruments") {
                parameter("exchange", "Binance")
            }.body()
            val instruments = Json { ignoreUnknownKeys = true }.decodeFromString<List<InstrumentData>>(response)
            if (instruments.isNotEmpty()) symbol = instruments.first().symbol
        } catch (_: Exception) {}
    }

    private suspend fun loadCandles() {
        loading = true
        try {
            val response: String = httpClient.get("$baseUrl/api/footprint") {
                parameter("exchange", "Binance")
                parameter("symbol", symbol)
                parameter("timeframe", timeframe)
                parameter("limit", 60)
            }.body()
            candles = Json { ignoreUnknownKeys = true }.decodeFromString(response)
        } catch (e: Exception) {
            // silently retry
        }
        loading = false
    }

    private fun render() {
        val g = ctx ?: return
        val w = canvas?.width ?: 800
        val h = canvas?.height ?: 600

        g.clearRect(0.0, 0.0, w.toDouble(), h.toDouble())
        g.fillStyle = "#0A0A0A"
        g.fillRect(0.0, 0.0, w.toDouble(), h.toDouble())

        if (loading) {
            g.fillStyle = "#4CAF50"; g.font = "14px monospace"; g.textAlign = "center"
            g.fillText("Loading footprint data...", w / 2.0, h / 2.0)
            return
        }
        if (candles.isEmpty()) {
            g.fillStyle = "#666666"; g.font = "14px monospace"; g.textAlign = "center"
            g.fillText("No data for $symbol / $timeframe", w / 2.0, h / 2.0)
            return
        }

        val margin = Margin(80.0, 25.0, 30.0, 55.0)
        val chartW = w - margin.left - margin.right
        val chartH = h - margin.top - margin.bottom
        if (chartW <= 0 || chartH <= 0) return

        val allPrices = candles.flatMap { cd -> cd.levels.map { it.price.toDouble() } }
        val minPrice = allPrices.minOrNull() ?: return
        val maxPrice = allPrices.maxOrNull() ?: return
        val priceRange = maxPrice - minPrice
        if (priceRange <= 0) return

        val allVolumes = candles.flatMap { cd -> cd.levels.flatMap { listOf(it.bidVolume.toDouble(), it.askVolume.toDouble()) } }
        val maxVol = allVolumes.maxOrNull() ?: 1.0
        if (maxVol <= 0) return

        val candleW = (chartW / candles.size).coerceAtMost(35.0)
        val candleSpacing = 1.0

        for ((i, candle) in candles.withIndex()) {
            val x = margin.left + i * (candleW + candleSpacing)
            val levels = candle.levels.sortedByDescending { it.price.toDouble() }
            if (levels.isEmpty()) continue
            val levelH = chartH / levels.size

            for ((j, level) in levels.withIndex()) {
                val py = margin.top + j * levelH
                val bidV = level.bidVolume.toDouble()
                val askV = level.askVolume.toDouble()
                val total = bidV + askV
                val volRatio = total / maxVol
                val barW = candleW * volRatio.coerceIn(0.08, 1.0)
                val bidRatio = bidV / total.coerceAtLeast(0.0001)
                
                g.fillStyle = when { bidRatio > 0.55 -> "#4CAF50"; bidRatio > 0.45 -> "#888888"; else -> "#F44336" }
                g.fillRect(x, py, barW, levelH.coerceAtLeast(1.5))
                g.strokeStyle = if (level.price.toDouble() > candle.levels.first().price.toDouble()) "#4CAF5040" else "#F4433640"
                g.lineWidth = 0.5
                g.strokeRect(x, py, barW, levelH.coerceAtLeast(1.5))
            }
        }

        // Price labels
        g.fillStyle = "#888888"; g.font = "10px monospace"; g.textAlign = "right"
        for (i in 0..8) {
            val py = margin.top + chartH * i / 8
            val price = maxPrice - (py - margin.top) / chartH * priceRange
            g.fillText(formatPrice(price), margin.left - 6, py + 4)
        }

        // Time labels
        g.textAlign = "center"
        val step = max(1, candles.size / 6).toInt()
        for (i in candles.indices step step) {
            val x = margin.left + i * (candleW + candleSpacing) + candleW / 2
            g.fillText(formatTime(candles[i].startTime), x, h - margin.bottom + 14)
        }

        // Title
        g.fillStyle = "#E0E0E0"; g.font = "bold 12px monospace"; g.textAlign = "left"
        g.fillText("$symbol @ $timeframe  |  ${candles.size} candles", margin.left, 16.0)
    }

    private fun formatPrice(v: Double): String {
        val d = when { v >= 100000 -> 1; v >= 10000 -> 1; v >= 1000 -> 2; v >= 1 -> 2; else -> 4 }
        val factor = 10.0.pow(d); val r = round(v * factor) / factor
        val parts = r.toString().split(".")
        return "${parts[0]}.${(parts.getOrElse(1) { "" }).padEnd(d, '0').take(d)}"
    }

    private fun formatTime(ts: Long): String {
        return try {
            val inst = Instant.fromEpochMilliseconds(ts)
            val local = inst.toLocalDateTime(TimeZone.currentSystemDefault())
            "${local.hour.toString().padStart(2,'0')}:${local.minute.toString().padStart(2,'0')}"
        } catch (_: Exception) { "" }
    }

    data class Margin(val left: Double, val top: Double, val right: Double, val bottom: Double)
}

@Serializable
data class FootprintCandleData(
    val exchange: String = "", val symbol: String = "", val timeframe: String = "",
    val startTime: Long = 0L, val endTime: Long = 0L, val totalTicks: Long = 0L,
    val minPrice: String = "0", val maxPrice: String = "0",
    val levels: List<FootprintLevelData> = emptyList()
)

@Serializable
data class FootprintLevelData(
    val price: String = "0", val bidVolume: String = "0", val askVolume: String = "0",
    val bidCount: Int = 0, val askCount: Int = 0
)

@Serializable
data class InstrumentData(val symbol: String = "", val start: Long = 0L, val end: Long = 0L, val candles: Long = 0L)
