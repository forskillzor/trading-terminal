package http

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.TimeUnit
import config.Config

class HttpClient {
    private val logging = HttpLoggingInterceptor().apply {
        level = if (Config.VERBOSE) HttpLoggingInterceptor.Level.BODY else HttpLoggingInterceptor.Level.NONE
    }

    private val client = OkHttpClient.Builder()
        .addInterceptor(logging)
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val mapper = ObjectMapper().apply {
        registerModule(KotlinModule.Builder().build())
        configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
    }

    private val JSON = "application/json; charset=utf-8".toMediaType()

    private fun createRequest(url: String, method: String = "GET", body: Any? = null): Request {
        val builder = Request.Builder()
            .url("${Config.YOUTRACK_URL}/api/$url")
            .addHeader("Accept", "application/json")
            .addHeader("Content-Type", "application/json")
            .addHeader("Authorization", "Bearer ${Config.TOKEN}")

        if (body != null) {
            builder.method(method, mapper.writeValueAsString(body).toRequestBody(JSON))
        } else {
            builder.method(method, null)
        }

        return builder.build()
    }

    private fun executeRequest(request: Request): String? {
        return try {
            val response = client.newCall(request).execute()
            val body = response.body?.string()

            if (!response.isSuccessful) {
                println("❌ HTTP ${response.code}: ${response.message}")
                if (Config.VERBOSE && body != null) {
                    println("   Response: $body")
                }
                return null
            }
            body
        } catch (e: Exception) {
            println("❌ Ошибка запроса: ${e.message}")
            null
        }
    }

    fun get(url: String): String? = executeRequest(createRequest(url, "GET"))
    fun post(url: String, body: Any): String? = executeRequest(createRequest(url, "POST", body))
    fun put(url: String, body: Any): String? = executeRequest(createRequest(url, "PUT", body))
}