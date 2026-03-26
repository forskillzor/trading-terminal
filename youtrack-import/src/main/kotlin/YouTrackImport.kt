
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.readValue
import kotlinx.datetime.*
import okhttp3.Credentials
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import java.io.File
import java.util.concurrent.TimeUnit

// ==================== КОНФИГУРАЦИЯ ====================

object Config {
    val YOUTRACK_URL = System.getenv("YOUTRACK_URL") ?: "http://localhost:8080"
    val PROJECT_ID = System.getenv("YOUTRACK_PROJECT") ?: "NOUS"
    val TOKEN = System.getenv("YOUTRACK_TOKEN")
    val USER = System.getenv("YOUTRACK_USER")
    val PASS = System.getenv("YOUTRACK_PASS")

    // Даты спринтов (начиная с сегодняшнего дня + 1 день)
    val SPRINT_START = Clock.System.todayAt(TimeZone.currentSystemDefault()).plus(1, DateTimeUnit.DAY)
    val SPRINT_DURATION_DAYS = 14 // 2 недели

    // Режимы
    val DRY_RUN = System.getenv("YOUTRACK_DRY_RUN")?.toBoolean() ?: false
    val VERBOSE = System.getenv("YOUTRACK_VERBOSE")?.toBoolean() ?: true
}

// ==================== МОДЕЛИ ДАННЫХ ====================

data class YouTrackImport(
    val project: ProjectConfig,
    val components: List<Component>,
    val epics: List<Epic>,
    val tasks: List<Task>,
    val sprints: List<Sprint>,
    val workflow: Workflow
)

data class ProjectConfig(
    val id: String,
    val name: String,
    val description: String
)

data class Component(
    val id: String,
    val name: String,
    val description: String
)

data class Epic(
    val id: String,
    val summary: String,
    val description: String,
    val priority: String = "High",
    val sprintIds: List<String>
)

data class Task(
    val id: String,
    val summary: String,
    val description: String,
    val component: String,
    val type: String,
    val priority: String,
    val estimate: Int,
    val epic: String,
    val sprint: String,
    val tags: List<String> = emptyList(),
    val dependencies: List<String> = emptyList()
)

data class Sprint(
    val id: String,
    val name: String,
    val goal: String,
    val startDate: String,
    val endDate: String,
    val taskIds: List<String>,
    val epic: String
)

data class Workflow(
    val name: String,
    val states: List<String>,
    val transitions: List<Transition>
)

data class Transition(
    val from: String,
    val to: String,
    val name: String
)

// ==================== HTTP КЛИЕНТ ====================

class YouTrackClient {
    private val logging = HttpLoggingInterceptor().apply {
        level = if (Config.VERBOSE) HttpLoggingInterceptor.Level.BODY else HttpLoggingInterceptor.Level.BASIC
    }

    private val client = OkHttpClient.Builder()
        .addInterceptor(logging)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val mapper = ObjectMapper().registerModule(KotlinModule())
    private val JSON = "application/json; charset=utf-8".toMediaType()

    private fun createRequest(url: String, method: String = "GET", body: String? = null): Request {
        val builder = Request.Builder()
            .url("${Config.YOUTRACK_URL}/api/$url")
            .addHeader("Accept", "application/json")
            .addHeader("Content-Type", "application/json")

        when {
            Config.TOKEN != null -> builder.addHeader("Authorization", "Bearer ${Config.TOKEN}")
            Config.USER != null && Config.PASS != null -> {
                val credentials = Credentials.basic(Config.USER, Config.PASS)
                builder.addHeader("Authorization", credentials)
            }
        }

        if (body != null) {
            builder.method(method, body.toRequestBody(JSON))
        } else {
            builder.method(method, null)
        }

        return builder.build()
    }

    fun executeRequest(request: Request): String? {
        return try {
            val response = client.newCall(request).execute()
            val body = response.body?.string()
            if (!response.isSuccessful) {
                println("❌ HTTP ${response.code}: ${response.message}")
                if (Config.VERBOSE) println("   Response: $body")
                return null
            }
            body
        } catch (e: Exception) {
            println("❌ Ошибка: ${e.message}")
            null
        }
    }

    fun testConnection(): Boolean {
        val request = createRequest("admin/projects")
        return executeRequest(request) != null
    }

    fun createComponent(projectId: String, component: Component): Boolean {
        if (Config.DRY_RUN) {
            println("   [DRY RUN] Создать компонент: ${component.name}")
            return true
        }

        // Используем mapper для безопасного создания JSON с экранированием
        val json = mapper.writeValueAsString(mapOf(
            "name" to component.name,
            "description" to component.description
        ))

        val request = createRequest(
            "admin/projects/$projectId/customFields/Components/items", // Components с большой буквы
            "POST",
            json
        )
        return executeRequest(request) != null
    }

    fun createIssue(projectId: String, task: Task, epicId: String? = null): String? {
        if (Config.DRY_RUN) {
            println("   [DRY RUN] Создать задачу: ${task.id} - ${task.summary}")
            return task.id
        }

        val customFields = mutableListOf<Map<String, Any>>()

        customFields.add(mapOf(
            "name" to "Type",
            "\$type" to "SingleEnumIssueCustomField",
            "value" to mapOf("name" to task.type)
        ))

        customFields.add(mapOf(
            "name" to "Priority",
            "\$type" to "SingleEnumIssueCustomField",
            "value" to mapOf("name" to task.priority)
        ))

        customFields.add(mapOf(
            "name" to "Components",
            "\$type" to "MultiEnumIssueCustomField",
            "value" to listOf(mapOf("name" to task.component))
        ))

        if (task.estimate > 0) {
            customFields.add(mapOf(
                "name" to "Estimation",
                "\$type" to "SimpleIssueCustomField",
                "value" to mapOf("minutes" to task.estimate * 60)
            ))
        }

        if (epicId != null) {
            customFields.add(mapOf(
                "name" to "Epic",
                "\$type" to "SingleIssueCustomField",
                "value" to mapOf("id" to epicId)
            ))
        }

        if (task.tags.isNotEmpty()) {
            customFields.add(mapOf(
                "name" to "Tags",
                "\$type" to "MultiIssueCustomField",
                "value" to task.tags.map { mapOf("name" to it) }
            ))
        }

        val taskJson = mapper.writeValueAsString(mapOf(
            "project" to mapOf("id" to projectId),
            "summary" to task.summary,
            "description" to task.description,
            "customFields" to customFields
        ))

        val request = createRequest("issues", "POST", taskJson)
        val response = executeRequest(request)

        return if (response != null) {
            val createdIssue = mapper.readValue<Map<String, Any>>(response)
            createdIssue["id"] as String
        } else null
    }

    fun createSprint(projectId: String, sprint: Sprint): String? {
        if (Config.DRY_RUN) {
            println("   [DRY RUN] Создать спринт: ${sprint.name}")
            return sprint.id
        }

        // Используем mapper для безопасного создания JSON
        val json = mapper.writeValueAsString(mapOf(
            "name" to sprint.name,
            "goal" to sprint.goal,
            "start" to sprint.startDate,
            "end" to sprint.endDate,
            "project" to mapOf("id" to projectId)
        ))

        // В YouTrack спринты создаются в рамках Agile-досок
        // Сначала нужно получить ID agile-доски для проекта
        // Упрощенный подход: используем первую доступную agile-доску
        val request = createRequest("agiles?fields=id,name&query=project:$projectId", "GET", null)
        val agilesResponse = executeRequest(request)
        
        if (agilesResponse == null) {
            println("   ⚠️ Не удалось получить Agile-доски для проекта $projectId")
            println("   ⚠️ Создание спринта пропущено: ${sprint.name}")
            return null
        }
        
        val agiles = mapper.readValue<List<Map<String, Any>>>(agilesResponse)
        if (agiles.isEmpty()) {
            println("   ⚠️ Нет Agile-досок для проекта $projectId")
            println("   ⚠️ Создание спринта пропущено: ${sprint.name}")
            return null
        }
        
        val agileId = agiles.first()["id"] as String
        val sprintRequest = createRequest("agiles/$agileId/sprints", "POST", json)
        val response = executeRequest(sprintRequest)

        return if (response != null) {
            val createdSprint = mapper.readValue<Map<String, Any>>(response)
            createdSprint["id"] as String
        } else null
    }

    fun linkTaskToSprint(taskId: String, sprintId: String): Boolean {
        if (Config.DRY_RUN) {
            println("   [DRY RUN] Привязать $taskId к спринту $sprintId")
            return true
        }

        // В YouTrack API для обновления поля Sprint используется PATCH запрос
        // с полным объектом кастомного поля
        val json = mapper.writeValueAsString(mapOf(
            "\$type" to "SprintIssueCustomField",
            "value" to mapOf("id" to sprintId)
        ))

        val request = createRequest(
            "issues/$taskId/customFields/Sprint?fields=id",
            "POST",  // YouTrack использует POST для обновления кастомных полей
            json
        )
        return executeRequest(request) != null
    }
}

// ==================== ГЕНЕРАЦИЯ ДАННЫХ ====================

object DataGenerator {

    fun generateImportData(): YouTrackImport {
        return YouTrackImport(
            project = ProjectConfig(
                id = "NOUS",
                name = "Nous Platform",
                description = "Kotlin Multiplatform trading platform with plugin system"
            ),
            components = generateComponents(),
            epics = generateEpics(),
            tasks = generateTasks(),
            sprints = generateSprints(),
            workflow = generateWorkflow()
        )
    }

    private fun generateComponents(): List<Component> {
        return listOf(
            // Core
            Component("Core-Base", "Core-Base", "utils, extensions, math"),
            Component("Core-Domain", "Core-Domain", "entities, repositories, usecases"),
            Component("Core-Network", "Core-Network", "http, websocket, client"),
            Component("Core-DI", "Core-DI", "CoreModule, DI"),
            Component("Core-Theme", "Core-Theme", "Colors, Typography, Theme"),

            // Features
            Component("Feature-Chart", "Feature-Chart", "графики, свечи, индикаторы"),
            Component("Feature-DOM", "Feature-DOM", "стакан заявок"),
            Component("Feature-Trades", "Feature-Trades", "лента сделок"),
            Component("Feature-Terminal", "Feature-Terminal", "лейаут, панели"),
            Component("Feature-Portfolio", "Feature-Portfolio", "портфель, позиции"),
            Component("Feature-Indicators", "Feature-Indicators", "индикаторы"),
            Component("Feature-Strategies", "Feature-Strategies", "стратегии"),
            Component("Feature-Backtest", "Feature-Backtest", "бэктестинг"),
            Component("Feature-Editor", "Feature-Editor", "редактор кода"),
            Component("Feature-REPL", "Feature-REPL", "REPL консоли"),

            // Plugin System
            Component("Plugin-Loader", "Plugin-Loader", "сканирование JAR"),
            Component("ClassLoader", "ClassLoader", "изолированные ClassLoader'ы"),
            Component("Security", "Security", "подписи, разрешения"),
            Component("Registry", "Registry", "PluginRegistry, PluginManifest"),

            // Public API
            Component("Market-API", "Market-API", "MarketDataProvider"),
            Component("Trading-API", "Trading-API", "TradingProvider"),
            Component("Account-API", "Account-API", "AccountProvider"),
            Component("Indicators-API", "Indicators-API", "Indicator"),
            Component("Strategies-API", "Strategies-API", "Strategy"),
            Component("Robots-API", "Robots-API", "TradingRobot"),
            Component("UI-API", "UI-API", "Widget, ChartWidget"),

            // Providers
            Component("Binance-Provider", "Binance-Provider", "Binance провайдер"),
            Component("Bybit-Provider", "Bybit-Provider", "Bybit провайдер"),

            // Infrastructure
            Component("Build-System", "Build-System", "Gradle, KMP"),
            Component("CI-CD", "CI/CD", "GitHub Actions"),
            Component("Database", "Database", "SQLite"),
            Component("Monitoring-Logging", "Monitoring/Logging", "логирование")
        )
    }

    private fun generateEpics(): List<Epic> {
        return listOf(
            Epic(
                id = "NOUS-EPIC-1",
                summary = "Этап 1: Фундамент и архитектура",
                description = "Базовая модульная архитектура KMP + Clean Architecture + DI + подключение к Binance WebSocket",
                priority = "High",
                sprintIds = listOf("Sprint 1", "Sprint 2")
            ),
            Epic(
                id = "NOUS-EPIC-2",
                summary = "Этап 2: MVP Read-Only",
                description = "DOM, Chart, Trades виджеты + базовый UI терминала + получение данных в реальном времени",
                priority = "High",
                sprintIds = listOf("Sprint 3", "Sprint 4")
            ),
            Epic(
                id = "NOUS-EPIC-3",
                summary = "Этап 3: Live Trading MVP",
                description = "Авторизация API ключей + размещение ордеров + портфель + paper trading",
                priority = "High",
                sprintIds = listOf("Sprint 5", "Sprint 6")
            ),
            Epic(
                id = "NOUS-EPIC-4",
                summary = "Этап 4: Запуск и первые пользователи",
                description = "Публичный релиз + waitlist + сбор фидбека + быстрые итерации",
                priority = "High",
                sprintIds = listOf("Sprint 7", "Sprint 8")
            ),
            Epic(
                id = "NOUS-EPIC-5",
                summary = "Этап 5: Монетизация и масштабирование",
                description = "Pro подписка + маркетплейс плагинов + новые биржи + мобильные приложения",
                priority = "Medium",
                sprintIds = listOf("Sprint 9", "Sprint 10", "Sprint 11")
            )
        )
    }

    private fun generateTasks(): List<Task> {
        val tasks = mutableListOf<Task>()

        // ===== Sprint 1: KMP Foundation (40 points) =====
        tasks.addAll(listOf(
            Task("NOUS-1", "Настроить KMP проект с Gradle", "Создать базовую структуру KMP проекта с поддержкой JVM", "Build-System", "Task", "High", 5, "NOUS-EPIC-1", "Sprint 1"),
            Task("NOUS-2", "Создать Clean Architecture слои", "Реализовать domain, data, presentation слои", "Core-Domain", "Task", "High", 8, "NOUS-EPIC-1", "Sprint 1"),
            Task("NOUS-3", "Реализовать Core-Network", "HTTP клиент, WebSocket клиент, обработка ошибок", "Core-Network", "Feature", "High", 13, "NOUS-EPIC-1", "Sprint 1"),
            Task("NOUS-4", "Настроить DI с Koin", "Dependency injection для всех слоев", "Core-DI", "Task", "Medium", 5, "NOUS-EPIC-1", "Sprint 1"),
            Task("NOUS-5", "Создать тему приложения", "Compose Desktop тема с цветами и типографикой", "Core-Theme", "Feature", "Medium", 8, "NOUS-EPIC-1", "Sprint 1")
        ))

        // ===== Sprint 2: Binance Integration (45 points) =====
        tasks.addAll(listOf(
            Task("NOUS-6", "BinanceMarketProvider", "Получение рыночных данных с Binance", "Binance-Provider", "Feature", "High", 13, "NOUS-EPIC-1", "Sprint 2"),
            Task("NOUS-7", "WebSocket подключение к Binance", "Стабильное подключение с реконнектами", "Binance-Provider", "Feature", "High", 8, "NOUS-EPIC-1", "Sprint 2"),
            Task("NOUS-8", "Адаптеры моделей Binance", "Преобразование моделей Binance", "Binance-Provider", "Task", "Medium", 5, "NOUS-EPIC-1", "Sprint 2"),
            Task("NOUS-9", "Создать Market-API", "Интерфейсы для рыночных данных", "Market-API", "Task", "High", 5, "NOUS-EPIC-1", "Sprint 2"),
            Task("NOUS-10", "Аутентификация API ключей", "Безопасное хранение API ключей", "Binance-Provider", "Feature", "High", 8, "NOUS-EPIC-1", "Sprint 2"),
            Task("NOUS-11", "Unit тесты Core", "Тесты для domain слоя", "Core-Domain", "Task", "Medium", 5, "NOUS-EPIC-1", "Sprint 2")
        ))

        // ===== Sprint 3: DOM Widget (50 points) =====
        tasks.addAll(listOf(
            Task("NOUS-12", "Реализовать DomWidget", "Виджет стакана заявок с анимацией", "Feature-DOM", "Feature", "High", 13, "NOUS-EPIC-2", "Sprint 3"),
            Task("NOUS-13", "Создать DomViewModel", "ViewModel для DOM с состоянием", "Feature-DOM", "Feature", "High", 8, "NOUS-EPIC-2", "Sprint 3"),
            Task("NOUS-14", "Реализовать DomSectionNinja", "Расширенный вид DOM с объемами", "Feature-DOM", "Feature", "Medium", 13, "NOUS-EPIC-2", "Sprint 3"),
            Task("NOUS-15", "Добавить DepthLimitSelector", "Выбор глубины стакана (5-500 уровней)", "Feature-DOM", "Feature", "Low", 3, "NOUS-EPIC-2", "Sprint 3"),
            Task("NOUS-16", "Создать DomHeader", "Заголовок DOM со спредом", "Feature-DOM", "Feature", "Medium", 5, "NOUS-EPIC-2", "Sprint 3"),
            Task("NOUS-17", "Полировка DOM визуализации", "Улучшение производительности DOM", "Feature-DOM", "Improvement", "Medium", 8, "NOUS-EPIC-2", "Sprint 3")
        ))

        // ===== Sprint 4: Chart & Trades (50 points) =====
        tasks.addAll(listOf(
            Task("NOUS-18", "CandleStickChartWidget", "Свечной график с таймфреймами", "Feature-Chart", "Feature", "High", 21, "NOUS-EPIC-2", "Sprint 4"),
            Task("NOUS-19", "Создать ChartViewModel", "ViewModel для графика с историческими данными", "Feature-Chart", "Feature", "High", 13, "NOUS-EPIC-2", "Sprint 4"),
            Task("NOUS-20", "Поддержка таймфреймов", "1m, 5m, 15m, 1h, 4h, 1d", "Feature-Chart", "Feature", "Medium", 8, "NOUS-EPIC-2", "Sprint 4"),
            Task("NOUS-21", "TradesWidget", "Лента сделок в реальном времени", "Feature-Trades", "Feature", "High", 13, "NOUS-EPIC-2", "Sprint 4"),
            Task("NOUS-22", "Создать TradesViewModel", "ViewModel для ленты сделок", "Feature-Trades", "Feature", "High", 8, "NOUS-EPIC-2", "Sprint 4"),
            Task("NOUS-23", "Базовые индикаторы", "SMA, EMA, VWAP на графике", "Feature-Indicators", "Feature", "Medium", 8, "NOUS-EPIC-2", "Sprint 4")
        ))

        // ===== Sprint 5: Terminal Layout (55 points) =====
        tasks.addAll(listOf(
            Task("NOUS-24", "Создать TerminalLayout", "Основной layout с перетаскиваемыми панелями", "Feature-Terminal", "Feature", "High", 21, "NOUS-EPIC-3", "Sprint 5"),
            Task("NOUS-25", "Реализовать панели", "Chart, DOM, Trades панели", "Feature-Terminal", "Feature", "High", 13, "NOUS-EPIC-3", "Sprint 5"),
            Task("NOUS-26", "Авторизация по API-ключам", "UI для ввода и хранения ключей", "Binance-Provider", "Feature", "High", 8, "NOUS-EPIC-3", "Sprint 5"),
            Task("NOUS-27", "Размещение ордеров", "Limit/Market ордера через UI", "Feature-Portfolio", "Feature", "High", 13, "NOUS-EPIC-3", "Sprint 5")
        ))

        // ===== Sprint 6: Portfolio & Trading (50 points) =====
        tasks.addAll(listOf(
            Task("NOUS-28", "Отображение баланса", "Баланс и позиции в реальном времени", "Feature-Portfolio", "Feature", "High", 8, "NOUS-EPIC-3", "Sprint 6"),
            Task("NOUS-29", "Базовый риск-менеджмент", "Stop-loss, take-profit", "Feature-Portfolio", "Feature", "High", 13, "NOUS-EPIC-3", "Sprint 6"),
            Task("NOUS-30", "Paper trading режим", "Торговля на демо-счете", "Feature-Portfolio", "Feature", "Medium", 13, "NOUS-EPIC-3", "Sprint 6"),
            Task("NOUS-31", "OrdersList панель", "Список активных ордеров", "Feature-Portfolio", "Feature", "Medium", 8, "NOUS-EPIC-3", "Sprint 6"),
            Task("NOUS-32", "PositionsList панель", "Список открытых позиций", "Feature-Portfolio", "Feature", "Medium", 8, "NOUS-EPIC-3", "Sprint 6")
        ))

        // ===== Sprint 7: Launch Prep (45 points) =====
        tasks.addAll(listOf(
            Task("NOUS-33", "Публичный релиз", "Подготовка к публичному запуску", "CI-CD", "Task", "High", 13, "NOUS-EPIC-4", "Sprint 7"),
            Task("NOUS-34", "Waitlist лендинг", "Страница для сбора email", "Feature-Terminal", "Feature", "Medium", 8, "NOUS-EPIC-4", "Sprint 7"),
            Task("NOUS-35", "Сбор фидбека", "Интеграция с системой фидбека", "Monitoring-Logging", "Feature", "Medium", 8, "NOUS-EPIC-4", "Sprint 7"),
            Task("NOUS-36", "Быстрые итерации", "Процесс быстрых исправлений", "Build-System", "Task", "High", 13, "NOUS-EPIC-4", "Sprint 7"),
            Task("NOUS-37", "Документация API", "Полная документация public-api", "Market-API", "Task", "Medium", 8, "NOUS-EPIC-4", "Sprint 7")
        ))

        // ===== Sprint 8: User Growth (45 points) =====
        tasks.addAll(listOf(
            Task("NOUS-38", "Контент-маркетинг", "YouTube, Telegram, Twitter", "Monitoring-Logging", "Task", "Medium", 8, "NOUS-EPIC-4", "Sprint 8"),
            Task("NOUS-39", "Первые 100 пользователей", "Онбординг и поддержка", "Feature-Terminal", "Feature", "High", 13, "NOUS-EPIC-4", "Sprint 8"),
            Task("NOUS-40", "Приоритизация фидбека", "Система приоритизации задач", "Monitoring-Logging", "Task", "Medium", 8, "NOUS-EPIC-4", "Sprint 8"),
            Task("NOUS-41", "Финальный рефакторинг", "Рефакторинг перед запуском", "Core-Domain", "Improvement", "High", 13, "NOUS-EPIC-4", "Sprint 8"),
            Task("NOUS-42", "CI/CD пайплайн", "GitHub Actions для сборки", "CI-CD", "Task", "Medium", 8, "NOUS-EPIC-4", "Sprint 8")
        ))

        // ===== Sprint 9: Monetization (50 points) =====
        tasks.addAll(listOf(
            Task("NOUS-43", "Pro подписка", "Система подписки $29.9/мес", "Feature-Portfolio", "Feature", "High", 21, "NOUS-EPIC-5", "Sprint 9"),
            Task("NOUS-44", "Первые платящие", "Конверсия 20-50 пользователей", "Monitoring-Logging", "Task", "High", 13, "NOUS-EPIC-5", "Sprint 9"),
            Task("NOUS-45", "Plugin SDK", "SDK для разработчиков плагинов", "Plugin-Loader", "Feature", "High", 21, "NOUS-EPIC-5", "Sprint 9"),
            Task("NOUS-46", "Загрузка плагинов JAR", "Динамическая загрузка из /plugins", "Plugin-Loader", "Feature", "High", 13, "NOUS-EPIC-5", "Sprint 9")
        ))

        // ===== Sprint 10: Marketplace (50 points) =====
        tasks.addAll(listOf(
            Task("NOUS-47", "Маркетплейс плагинов", "Система рейтингов и продаж", "Registry", "Feature", "High", 21, "NOUS-EPIC-5", "Sprint 10"),
            Task("NOUS-48", "SandboxClassLoader", "Изоляция плагинов", "Security", "Feature", "High", 13, "NOUS-EPIC-5", "Sprint 10"),
            Task("NOUS-49", "SignatureVerifier", "Проверка цифровых подписей", "Security", "Feature", "Medium", 8, "NOUS-EPIC-5", "Sprint 10"),
            Task("NOUS-50", "Bybit Provider", "Поддержка Bybit биржи", "Bybit-Provider", "Feature", "High", 21, "NOUS-EPIC-5", "Sprint 10")
        ))

        // ===== Sprint 11: Scale (40 points) =====
        tasks.addAll(listOf(
            Task("NOUS-51", "DSL для индикаторов", "Предметно-ориентированный язык", "Indicators-API", "Feature", "High", 21, "NOUS-EPIC-5", "Sprint 11"),
            Task("NOUS-52", "Бэктестинг движок", "Запуск стратегий на истории", "Feature-Backtest", "Feature", "High", 21, "NOUS-EPIC-5", "Sprint 11"),
            Task("NOUS-53", "Визуализация бэктеста", "Отображение сделок на графике", "Feature-Backtest", "Feature", "Medium", 13, "NOUS-EPIC-5", "Sprint 11"),
            Task("NOUS-54", "Торговый дневник", "SQLDelight для истории сделок", "Database", "Feature", "Medium", 13, "NOUS-EPIC-5", "Sprint 11"),
            Task("NOUS-55", "Релизный пайплайн", "Автоматические релизы", "CI-CD", "Task", "Medium", 8, "NOUS-EPIC-5", "Sprint 11")
        ))

        return tasks
    }

    private fun generateSprints(): List<Sprint> {
        val sprints = mutableListOf<Sprint>()
        var currentDate = Config.SPRINT_START

        val sprintData = listOf(
            "Sprint 1" to "KMP Foundation" to "Базовая архитектура KMP + Gradle + DI",
            "Sprint 2" to "Binance Integration" to "Подключение к Binance WebSocket + REST",
            "Sprint 3" to "DOM Widget" to "Стакан заявок с визуализацией",
            "Sprint 4" to "Chart & Trades" to "График + лента сделок",
            "Sprint 5" to "Terminal Layout" to "Терминальный интерфейс + панели",
            "Sprint 6" to "Portfolio & Trading" to "Портфель + размещение ордеров",
            "Sprint 7" to "Launch Prep" to "Подготовка к публичному запуску",
            "Sprint 8" to "User Growth" to "Первые 100 пользователей",
            "Sprint 9" to "Monetization" to "Pro подписка + первые платящие",
            "Sprint 10" to "Marketplace" to "Маркетплейс плагинов",
            "Sprint 11" to "Scale" to "DSL + бэктестинг + масштабирование"
        )

        for ((i, data) in sprintData.withIndex()) {
            val (sprintId, sprintName, sprintGoal) = data
            val endDate = currentDate.plus(Config.SPRINT_DURATION_DAYS, DateTimeUnit.DAY)

            sprints.add(
                Sprint(
                    id = sprintId,
                    name = "Sprint ${i + 1} - $sprintName",
                    goal = sprintGoal,
                    startDate = currentDate.toString(),
                    endDate = endDate.toString(),
                    taskIds = generateTasks().filter { it.sprint == sprintId }.map { it.id },
                    epic = "NOUS-EPIC-${(i / 2) + 1}"
                )
            )

            currentDate = endDate.plus(1, DateTimeUnit.DAY)
        }

        return sprints
    }

    private fun generateWorkflow(): Workflow {
        return Workflow(
            name = "Development Workflow",
            states = listOf("Backlog", "To Do", "In Progress", "Code Review", "Testing", "Done"),
            transitions = listOf(
                Transition("Backlog", "To Do", "Ready for work"),
                Transition("To Do", "In Progress", "Start work"),
                Transition("In Progress", "Code Review", "Submit for review"),
                Transition("Code Review", "Testing", "Approve"),
                Transition("Code Review", "In Progress", "Request changes"),
                Transition("Testing", "Done", "Pass testing"),
                Transition("Testing", "In Progress", "Fail testing")
            )
        )
    }
}

// ==================== MAIN ====================

fun main() {
    println("🚀 Nous Platform - YouTrack Import Tool")
    println("=" .repeat(60))

    if (Config.DRY_RUN) {
        println("⚠️  DRY RUN MODE - ничего не будет создано в YouTrack")
        println("=" .repeat(60))
    }

    // Проверка аутентификации
    if (Config.TOKEN == null && (Config.USER == null || Config.PASS == null)) {
        println("❌ Ошибка: Не настроена аутентификация")
        println("""
           Установи переменные окружения:
           - YOUTRACK_TOKEN (перманентный токен) ИЛИ
           - YOUTRACK_USER и YOUTRACK_PASS (базовая аутентификация)
           Опционально: YOUTRACK_URL, YOUTRACK_PROJECT
           
           Пример:
           export YOUTRACK_URL="http://localhost:8080"
           export YOUTRACK_PROJECT="NOUS"
           export YOUTRACK_TOKEN="perm:YOUR_TOKEN_HERE"
        """.trimIndent())
        System.exit(1)
    }

    val client = YouTrackClient()

    // Тест подключения
    println("\n🔍 Проверка подключения к YouTrack...")
    if (!client.testConnection()) {
        println("❌ Не удалось подключиться к YouTrack")
        println("   Проверь URL, токен и доступность сервера")
        System.exit(1)
    }
    println("✅ Подключение успешно: ${Config.YOUTRACK_URL}")

    // Генерация данных
    println("\n📊 Генерация данных для импорта...")
    val data = DataGenerator.generateImportData()
    println("   - Проект: ${data.project.name} (${data.project.id})")
    println("   - Компонентов: ${data.components.size}")
    println("   - Epic: ${data.epics.size}")
    println("   - Задач: ${data.tasks.size}")
    println("   - Спринтов: ${data.sprints.size}")
    println("   - Спринтов на Epic: ${data.sprints.size / data.epics.size} (в среднем)")

    // Создание компонентов
    println("\n🔧 Создание компонентов...")
    var componentsCreated = 0
    for (component in data.components) {
        if (client.createComponent(data.project.id, component)) {
            componentsCreated++
            if (Config.VERBOSE) print(".") else {}
        } else {
            if (Config.VERBOSE) print("x") else {}
        }
    }
    println("\n   ✅ Создано компонентов: $componentsCreated / ${data.components.size}")

    // Создание Epic
    println("\n🎯 Создание Epic...")
    val epicIds = mutableMapOf<String, String>()
    for (epic in data.epics) {
        val createdId = client.createIssue(data.project.id, Task(
            id = epic.id,
            summary = epic.summary,
            description = epic.description,
            component = "Core-Domain",
            type = "Epic",
            priority = epic.priority,
            estimate = 0,
            epic = "",
            sprint = ""
        ))
        if (createdId != null) {
            epicIds[epic.id] = createdId
            println("   ✅ ${epic.id}: $createdId")
        } else {
            println("   ❌ ${epic.id}: Ошибка")
        }
        Thread.sleep(200) // Rate limiting
    }

    // Создание задач
    println("\n📝 Создание задач...")
    var tasksCreated = 0
    val taskIds = mutableMapOf<String, String>()
    for (task in data.tasks) {
        val epicId = epicIds[task.epic]
        val createdId = client.createIssue(data.project.id, task, epicId)
        if (createdId != null) {
            tasksCreated++
            taskIds[task.id] = createdId
            if (tasksCreated % 10 == 0) {
                println("   ... $tasksCreated задач создано")
            }
        }
        Thread.sleep(300) // Rate limiting
    }
    println("   ✅ Создано задач: $tasksCreated / ${data.tasks.size}")

    // Создание спринтов
    println("\n📅 Создание спринтов...")
    val sprintIds = mutableMapOf<String, String>()
    for (sprint in data.sprints) {
        val sprintId = client.createSprint(data.project.id, sprint)
        if (sprintId != null) {
            sprintIds[sprint.id] = sprintId
            println("   ✅ ${sprint.name}: $sprintId")
            println("      📅 ${sprint.startDate} → ${sprint.endDate}")
            println("      📋 Задач: ${sprint.taskIds.size}")
        } else {
            println("   ❌ ${sprint.name}: Ошибка")
        }
        Thread.sleep(500) // Rate limiting
    }

    // Привязка задач к спринтам
    println("\n🔗 Привязка задач к спринтам...")
    var tasksLinked = 0
    for (task in data.tasks) {
        val sprintId = sprintIds[task.sprint]
        val taskId = taskIds[task.id]
        if (sprintId != null && taskId != null) {
            if (client.linkTaskToSprint(taskId, sprintId)) {
                tasksLinked++
            }
        }
    }
    println("   ✅ Привязано задач: $tasksLinked / ${data.tasks.size}")

    // Итоговая статистика
    println("\n" + "=" .repeat(60))
    println("🎉 Импорт завершён!")
    println("=" .repeat(60))
    println("""
        📊 Сводка:
        - Проект: ${data.project.id}
        - Компонентов: $componentsCreated / ${data.components.size}
        - Epic: ${epicIds.size} / ${data.epics.size}
        - Задач: $tasksCreated / ${data.tasks.size}
        - Спринтов: ${sprintIds.size} / ${data.sprints.size}
        - Задач в спринтах: $tasksLinked / ${data.tasks.size}
        
        📈 Распределение по Epic:
        ${data.epics.joinToString("\n") { epic ->
        val sprintCount = data.sprints.count { it.epic == epic.id }
        val taskCount = data.tasks.count { it.epic == epic.id }
        "   - ${epic.summary}: $sprintCount спринтов, $taskCount задач"
    }}
        
        🔗 Открой проект:
        ${Config.YOUTRACK_URL}/issues?q=project:${data.project.id}
        
        📋 Следующие шаги:
        1. Проверь задачи в YouTrack
        2. Настрой workflow состояния (если не создался автоматически)
        3. Открой первый спринт и начни работу!
        4. Используй Daily Standup шаблоны из templates/
        
        💡 Советы:
        - Обновляй статусы задач ежедневно
        - Веди time tracking для анализа производительности
        - Проводи ретроспективы после каждого спринта
    """.trimIndent())

    if (Config.DRY_RUN) {
        println("\n⚠️  DRY RUN MODE - данные не были созданы в YouTrack")
        println("   Убери YOUTRACK_DRY_RUN=true для реального импорта")
    }
}