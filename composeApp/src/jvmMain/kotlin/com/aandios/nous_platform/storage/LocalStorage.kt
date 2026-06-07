package com.aandios.nous_platform.storage

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.sql.Connection
import java.sql.DriverManager

/**
 * Локальное SQLite хранилище настроек и кеша.
 * Таблицы создаются автоматически при первом обращении.
 */
class LocalStorage(val dbPath: String = DEFAULT_PATH) {

    private val dbFile = File(dbPath)
    private var connection: Connection? = null

    private suspend fun getConnection(): Connection {
        val conn = connection
        if (conn != null && !conn.isClosed) return conn
        return withContext(Dispatchers.IO) {
            dbFile.parentFile?.mkdirs()
            val c = DriverManager.getConnection("jdbc:sqlite:$dbPath")
            c.createStatement().use { stmt ->
                stmt.execute("PRAGMA journal_mode=WAL")
                stmt.execute("PRAGMA synchronous=NORMAL")
            }
            ensureTables(c)
            connection = c
            c
        }
    }

    private fun ensureTables(conn: Connection) {
        conn.createStatement().use { stmt ->
            // Key-value store for settings
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS settings (
                    key TEXT PRIMARY KEY,
                    value TEXT NOT NULL
                )
            """)
            // Cached candles
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS candles_cache (
                    symbol TEXT NOT NULL,
                    timeframe TEXT NOT NULL,
                    timestamp INTEGER NOT NULL,
                    open REAL, high REAL, low REAL, close REAL, volume REAL,
                    PRIMARY KEY (symbol, timeframe, timestamp)
                )
            """)
            // Cached footprint candles (as JSON)
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS footprint_cache (
                    symbol TEXT NOT NULL,
                    start_time INTEGER NOT NULL,
                    end_time INTEGER NOT NULL,
                    json_data TEXT NOT NULL,
                    PRIMARY KEY (symbol, start_time)
                )
            """)
        }
    }

    // ============ Key-Value Settings ============

    suspend fun putString(key: String, value: String) {
        val conn = getConnection()
        conn.prepareStatement("INSERT OR REPLACE INTO settings (key, value) VALUES (?, ?)").use {
            it.setString(1, key)
            it.setString(2, value)
            it.execute()
        }
    }

    suspend fun getString(key: String): String? {
        val conn = getConnection()
        return conn.prepareStatement("SELECT value FROM settings WHERE key = ?").use {
            it.setString(1, key)
            val rs = it.executeQuery()
            if (rs.next()) rs.getString("value") else null
        }
    }

    suspend fun remove(key: String) {
        val conn = getConnection()
        conn.prepareStatement("DELETE FROM settings WHERE key = ?").use {
            it.setString(1, key)
            it.execute()
        }
    }

    // ============ Candles Cache ============

    suspend fun saveCandles(symbol: String, timeframe: String, candles: List<CandleCache>) {
        val conn = getConnection()
        conn.prepareStatement("INSERT OR REPLACE INTO candles_cache (symbol, timeframe, timestamp, open, high, low, close, volume) VALUES (?, ?, ?, ?, ?, ?, ?, ?)").use { stmt ->
            candles.forEach { c ->
                stmt.setString(1, symbol)
                stmt.setString(2, timeframe)
                stmt.setLong(3, c.timestamp)
                stmt.setDouble(4, c.open)
                stmt.setDouble(5, c.high)
                stmt.setDouble(6, c.low)
                stmt.setDouble(7, c.close)
                stmt.setDouble(8, c.volume)
                stmt.addBatch()
            }
            stmt.executeBatch()
        }
    }

    suspend fun getCandles(symbol: String, timeframe: String, limit: Int = 500): List<CandleCache> {
        val conn = getConnection()
        return conn.prepareStatement("SELECT * FROM candles_cache WHERE symbol = ? AND timeframe = ? ORDER BY timestamp ASC LIMIT ?").use {
            it.setString(1, symbol)
            it.setString(2, timeframe)
            it.setInt(3, limit)
            val rs = it.executeQuery()
            val result = mutableListOf<CandleCache>()
            while (rs.next()) {
                result.add(CandleCache(
                    rs.getLong("timestamp"),
                    rs.getDouble("open"), rs.getDouble("high"), rs.getDouble("low"),
                    rs.getDouble("close"), rs.getDouble("volume")
                ))
            }
            result
        }
    }

    suspend fun hasCandlesRange(symbol: String, timeframe: String, from: Long, to: Long): Boolean {
        val conn = getConnection()
        return conn.prepareStatement("SELECT COUNT(*) as cnt FROM candles_cache WHERE symbol = ? AND timeframe = ? AND timestamp >= ? AND timestamp <= ?").use {
            it.setString(1, symbol); it.setString(2, timeframe)
            it.setLong(3, from); it.setLong(4, to)
            val rs = it.executeQuery()
            rs.next() && rs.getInt("cnt") > 0
        }
    }

    // ============ Footprint Cache ============

    suspend fun saveFootprintCandles(symbol: String, candles: List<FootprintCache>) {
        val conn = getConnection()
        conn.prepareStatement("INSERT OR REPLACE INTO footprint_cache (symbol, start_time, end_time, json_data) VALUES (?, ?, ?, ?)").use { stmt ->
            candles.forEach { c ->
                stmt.setString(1, symbol)
                stmt.setLong(2, c.startTime)
                stmt.setLong(3, c.endTime)
                stmt.setString(4, c.jsonData)
                stmt.addBatch()
            }
            stmt.executeBatch()
        }
    }

    suspend fun getFootprintCandles(symbol: String, limit: Int = 60): List<FootprintCache> {
        val conn = getConnection()
        return conn.prepareStatement("SELECT * FROM footprint_cache WHERE symbol = ? ORDER BY start_time DESC LIMIT ?").use {
            it.setString(1, symbol); it.setInt(2, limit)
            val rs = it.executeQuery()
            val result = mutableListOf<FootprintCache>()
            while (rs.next()) {
                result.add(FootprintCache(rs.getLong("start_time"), rs.getLong("end_time"), rs.getString("json_data")))
            }
            result.reversed()
        }
    }

    // ============ Stats & Cleanup ============

    suspend fun getStats(): Map<String, Long> {
        val conn = getConnection()
        val result = mutableMapOf<String, Long>()
        return conn.createStatement().use { stmt ->
            result["settings"] = stmt.executeQuery("SELECT COUNT(*) FROM settings").use { it.next(); it.getLong(1) }
            result["candles"] = stmt.executeQuery("SELECT COUNT(*) FROM candles_cache").use { it.next(); it.getLong(1) }
            result["footprint"] = stmt.executeQuery("SELECT COUNT(*) FROM footprint_cache").use { it.next(); it.getLong(1) }
            result["db_size"] = dbFile.length()
            result
        }
    }

    suspend fun clearSettings() {
        getConnection().createStatement().use { it.execute("DELETE FROM settings") }
    }

    suspend fun clearCandles() {
        getConnection().createStatement().use { it.execute("DELETE FROM candles_cache") }
    }

    suspend fun clearFootprint() {
        getConnection().createStatement().use { it.execute("DELETE FROM footprint_cache") }
    }

    suspend fun clearAll() {
        getConnection().createStatement().use { stmt ->
            stmt.execute("DELETE FROM settings")
            stmt.execute("DELETE FROM candles_cache")
            stmt.execute("DELETE FROM footprint_cache")
        }
    }

    fun close() {
        connection?.close()
        connection = null
    }

    data class CandleCache(
        val timestamp: Long,
        val open: Double,
        val high: Double,
        val low: Double,
        val close: Double,
        val volume: Double
    )

    data class FootprintCache(
        val startTime: Long,
        val endTime: Long,
        val jsonData: String
    )

    companion object {
        val DEFAULT_PATH = "${System.getProperty("user.home")}/.nous/storage.db"
    }
}
