package com.aandios.nous_platform.data.api.bybit

import com.aandios.nous_platform.domain.entities.Candle
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

class BybitApi(private val client: HttpClient) {

    suspend fun getCandles(
        symbol: String,
        interval: String,
        limit: Int = 100
    ): List<BybitCandle> {
        // Используем правильный URL Bybit API v5
        val response: BybitKlineResponse = client.get("https://api.bybit.com/v5/market/kline") {
            // Параметры согласно документации Bybit
            parameter("category", "spot") // Для спотового рынка
            parameter("symbol", symbol.uppercase()) // Bybit требует uppercase
            parameter("interval", interval)
            parameter("limit", limit)
        }.body()

        // Проверяем успешность ответа
        if (response.retCode != 0) {
            throw Exception("Bybit API error: ${response.retMsg}")
        }

        // Маппим данные из ответа
        return response.result.list.map { rawCandle ->
            BybitCandle(
                openTime = rawCandle[0].toLong(),
                open = rawCandle[1],
                high = rawCandle[2],
                low = rawCandle[3],
                close = rawCandle[4],
                volume = rawCandle[5],
                // Bybit не возвращает все поля как Binance, используем дефолтные значения
                closeTime = 0L,
                quoteAssetVolume = "0",
                numberOfTrades = 0,
                takerBuyBaseAssetVolume = "0",
                takerBuyQuoteAssetVolume = "0"
            )
        }
    }
}

// Структура ответа Bybit API для kline
@Serializable
data class BybitKlineResponse(
    @SerialName("retCode") val retCode: Int,
    @SerialName("retMsg") val retMsg: String,
    @SerialName("result") val result: BybitKlineResult
)

@Serializable
data class BybitKlineResult(
    @SerialName("symbol") val symbol: String,
    @SerialName("category") val category: String,
    @SerialName("list") val list: List<List<String>>
)

@Serializable
data class BybitCandle(
    val openTime: Long,
    val open: String,
    val high: String,
    val low: String,
    val close: String,
    val volume: String,
    val closeTime: Long,
    val quoteAssetVolume: String,
    val numberOfTrades: Int,
    val takerBuyBaseAssetVolume: String,
    val takerBuyQuoteAssetVolume: String
) {
    fun toDomain(): Candle {
        return Candle(
            open = open.toFloat(),
            high = high.toFloat(),
            low = low.toFloat(),
            close = close.toFloat(),
            timestamp = openTime,
            volume = volume.toFloat()
        )
    }
}