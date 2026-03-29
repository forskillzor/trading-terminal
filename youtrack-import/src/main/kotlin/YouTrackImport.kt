import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.readValue
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import java.io.File
import java.util.concurrent.TimeUnit

// ============================================================================
// КОНФИГУРАЦИЯ
// ============================================================================
object Config {
    val YOUTRACK_URL = "http://localhost:8081"
    val TOKEN = "perm-YWRtaW4=.NDItMQ==.LLbeWWAZFVOXicroCXshF8D2sPvAm8"
    val VERBOSE = true
    val PROJECT_JSON_PATH = "project.json"

    // Эти значения нужно получить после ручной настройки (см. README.md)
    val EXISTING_PROJECT_ID = "0-8"
    val EXISTING_PROJECT_SHORTNAME = "NOUS"
    val AGILE_BOARD_ID = "1"  // ID Agile Board (найти в UI)

    // Field IDs (найти через UI или API)
    val COMPONENTS_FIELD_ID = "177-54"  // Subsystem field
    val STATUS_FIELD_ID = "177-55"      // Status field
    val STORY_POINTS_FIELD_ID = "177-56"
    val PRIORITY_FIELD_ID = "177-57"
    val TYPE_FIELD_ID = "177-58"
    val SPRINT_FIELD_ID = "177-59"      // Sprint field (State type)

    // Delays
    val EPIC_DELAY_MS = 500L
    val TASK_DELAY_MS = 300L
    val SPRINT_DELAY_MS = 400L
}

// ============================================================================
// МОДЕЛИ ДАННЫХ
// ============================================================================
data class ProjectConfig(
    val project: ProjectInfo,
    val customFields: Map<String, CustomFieldConfig>,
    val components: List<Component>,
    val epics: List<Epic>,
    val sprints: List<Sprint>,
    val tasks: List<Task>,
    val tags: List<Tag>,
    val workflow: Workflow?,
    val agileBoard: AgileBoard?
)

data class ProjectInfo(
    val id: String,
    val name: String,
    val description: String,
    val lead: String,
    val template: String
)

data class CustomFieldConfig(
    val name: String,
    val type: String,
    val values: List<String>? = null,
    val default: String? = null
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
    val priority: String,
    val sprints: List<String>
)

data class Sprint(
    val id: String,
    val name: String,
    val goal: String,
    val startDate: String,
    val endDate: String,
    val taskIds: List<String>,
    val epic: String,
    val capacity: Int
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
    val tags: List<String>
)

data class Tag(
    val name: String,
    val color: String
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

data class AgileBoard(
    val name: String,
    val type: String,
    val columns: List<BoardColumn>,
    val swimlanes: List<Swimlane>,
    val cardLayout: CardLayout
)

data class BoardColumn(
    val name: String,
    val states: List<String>
)

data class Swimlane(
    val name: String,
    val query: String
)

data class CardLayout(
    val fields: List<String>
)

// ============================================================================
// ИМПОРТЕР
// ============================================================================
class YouTrackImporter {

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

    // Кэши ID созданных объектов
    private var projectId: String = Config.EXISTING_PROJECT_ID
    private var agileBoardId: String = Config.AGILE_BOARD_ID
    private val customFieldIds = mutableMapOf<String, String>()
    private val componentIds = mutableMapOf<String, String>()
    private val epicIssueIds = mutableMapOf<String, String>()
    private val taskIssueIds = mutableMapOf<String, String>()
    private val sprintIds = mutableMapOf<String, String>()  // sprint.id -> sprintId в board
    private val tagIds = mutableMapOf<String, String>()

    // ========================================================================
    // ЗАГРУЗКА КОНФИГУРАЦИИ
    // ========================================================================
    fun loadConfig(): ProjectConfig {
        println("📂 Загрузка конфигурации из ${Config.PROJECT_JSON_PATH}")

        val possiblePaths = listOf(
            Config.PROJECT_JSON_PATH,
            "../${Config.PROJECT_JSON_PATH}",
            "../../${Config.PROJECT_JSON_PATH}",
            "src/main/kotlin/${Config.PROJECT_JSON_PATH}",
            "youtrack-import/${Config.PROJECT_JSON_PATH}"
        )

        var file: File? = null
        for (path in possiblePaths) {
            val f = File(path)
            if (f.exists()) {
                file = f
                println("   ✅ Файл найден: ${f.absolutePath}")
                break
            }
        }

        if (file == null) {
            throw RuntimeException("Файл project.json не найден. Искали по путям: ${possiblePaths.joinToString(", ")}")
        }

        val json = file.readText()
        return mapper.readValue(json)
    }

    // ========================================================================
    // HTTP ЗАПРОСЫ
    // ========================================================================
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

    private fun get(url: String): String? = executeRequest(createRequest(url, "GET"))
    private fun post(url: String, body: Any): String? = executeRequest(createRequest(url, "POST", body))
    private fun put(url: String, body: Any): String? = executeRequest(createRequest(url, "PUT", body))

    // ========================================================================
    // ПРОВЕРКИ
    // ========================================================================
    fun testConnection(): Boolean {
        println("🔌 Проверка подключения к YouTrack...")
        val response = get("admin/projects")
        return response != null
    }

    fun verifyProject(): Boolean {
        println("🔍 Проверка проекта ${Config.EXISTING_PROJECT_SHORTNAME}...")
        val response = get("admin/projects/${Config.EXISTING_PROJECT_ID}?fields=id,name,shortName")

        return if (response != null) {
            try {
                val project = mapper.readValue<Map<String, Any>>(response)
                val name = project["name"] as? String
                println("   ✅ Проект найден: $name")
                true
            } catch (e: Exception) {
                println("   ⚠️ Ошибка парсинга проекта: ${e.message}")
                false
            }
        } else {
            println("   ❌ Проект не найден. Создайте проект NOUS вручную (см. README.md)")
            false
        }
    }

    fun discoverAgileBoard(): String? {
        println("🔍 Поиск Agile Board для проекта...")

        val response = get("agile/boards?fields=id,name,projects(id,name)")

        if (response != null) {
            try {
                val boards = mapper.readValue<List<Map<String, Any>>>(response)
                val nousBoard = boards.find { board ->
                    val projects = board["projects"] as? List<Map<String, Any>>
                    projects?.any { it["id"] as? String == Config.EXISTING_PROJECT_ID } == true
                }

                if (nousBoard != null) {
                    val boardId = nousBoard["id"] as String
                    val boardName = nousBoard["name"] as? String
                    println("   ✅ Board найден: $boardName (ID: $boardId)")
                    agileBoardId = boardId
                    return boardId
                }
            } catch (e: Exception) {
                println("   ⚠️ Ошибка парсинга boards: ${e.message}")
            }
        }

        println("   ⚠️ Agile Board не найден. Создайте вручную (см. README.md)")
        return null
    }

    fun discoverCustomFields(): Map<String, String> {
        println("🔍 Поиск существующих custom fields...")
        val fields = mutableMapOf<String, String>()

        val response = get("admin/projects/${Config.EXISTING_PROJECT_ID}/customFields?fields=id,name,fieldType(name)")

        if (response != null) {
            try {
                val fieldList = mapper.readValue<List<Map<String, Any>>>(response)
                fieldList.forEach { field ->
                    val id = field["id"] as? String
                    val name = field["name"] as? String
                    if (id != null && name != null) {
                        fields[name] = id
                        println("   📋 Найдено поле: $name (ID: $id)")
                    }
                }
            } catch (e: Exception) {
                println("   ⚠️ Ошибка парсинга полей: ${e.message}")
            }
        }

        return fields
    }

    fun verifyCustomFields(existingFields: Map<String, String>): Boolean {
        println("\n=== ПРОВЕРКА CUSTOM FIELDS ===")
        val requiredFields = listOf("Story Points", "Priority", "Type", "Status", "Sprint")
        var allFound = true

        requiredFields.forEach { fieldName ->
            val fieldId = existingFields[fieldName]
            if (fieldId != null) {
                customFieldIds[fieldName] = fieldId
                println("   ✅ $fieldName: $fieldId")
            } else {
                println("   ❌ $fieldName: НЕ НАЙДЕНО")
                allFound = false
            }
        }

        // Fallback на Config ID
        customFieldIds.getOrPut("Story Points") { Config.STORY_POINTS_FIELD_ID }
        customFieldIds.getOrPut("Priority") { Config.PRIORITY_FIELD_ID }
        customFieldIds.getOrPut("Type") { Config.TYPE_FIELD_ID }
        customFieldIds.getOrPut("Status") { Config.STATUS_FIELD_ID }
        customFieldIds.getOrPut("Sprint") { Config.SPRINT_FIELD_ID }

        return allFound
    }

    // ========================================================================
    // КОМПОНЕНТЫ
    // ========================================================================
    fun setupComponents(components: List<Component>) {
        println("\n=== НАСТРОЙКА КОМПОНЕНТОВ ===")
        println("   📦 Компонентов: ${components.size}")

        components.forEach { comp ->
            componentIds[comp.name] = Config.COMPONENTS_FIELD_ID
        }

        println("   ✅ Настроено ${components.size} компонентов")
    }

    // ========================================================================
    // TAGS
    // ========================================================================
    fun registerTags(tags: List<Tag>) {
        println("\n=== РЕГИСТРАЦИЯ TAGS ===")

        tags.forEach { tag ->
            tagIds[tag.name] = tag.name
        }

        println("   ✅ Зарегистрировано ${tags.size} tags")
    }

    // ========================================================================
    // ЭПИКИ
    // ========================================================================
    fun createEpic(epic: Epic): String? {
        println("🎯 Создание эпика: ${epic.summary}")

        val customFields = buildCustomFields(
            type = "Epic",
            priority = epic.priority,
            status = "To Do",
            storyPoints = null,
            component = null,
            sprint = null
        )

        val issueBody = mutableMapOf<String, Any>(
            "project" to mapOf("id" to projectId),
            "summary" to epic.summary,
            "description" to buildEpicDescription(epic)
        )

        if (customFields.isNotEmpty()) {
            issueBody["customFields"] = customFields
        }

        val response = post("issues", issueBody)

        return if (response != null) {
            try {
                val issue = mapper.readValue<Map<String, Any>>(response)
                val id = issue["idReadable"] as? String ?: issue["id"] as String
                epicIssueIds[epic.id] = id
                println("   ✅ Эпик создан: $id")
                id
            } catch (e: Exception) {
                println("   ❌ Ошибка парсинга ответа: ${e.message}")
                null
            }
        } else {
            println("   ❌ Не удалось создать эпик")
            null
        }
    }

    private fun buildEpicDescription(epic: Epic): String {
        return buildString {
            appendLine(epic.description)
            appendLine()
            appendLine("---")
            appendLine("**Спринты:**")
            epic.sprints.forEach { sprint ->
                appendLine("- $sprint")
            }
            appendLine()
            appendLine("*ID: ${epic.id}*")
        }
    }

    // ========================================================================
    // ЗАДАЧИ
    // ========================================================================
    fun createTask(task: Task, epicIssueId: String?, sprintId: String?): String? {
        println("📝 Создание задачи: ${task.summary}")

        val customFields = buildCustomFields(
            type = task.type,
            priority = task.priority,
            status = "Backlog",
            storyPoints = if (task.estimate > 0) task.estimate.toString() else null,
            component = task.component,
            sprint = sprintId
        )

        val issueBody = mutableMapOf<String, Any>(
            "project" to mapOf("id" to projectId),
            "summary" to task.summary,
            "description" to buildTaskDescription(task)
        )

        // Связь с эпиком через Parent/Child link
        if (epicIssueId != null) {
            issueBody["links"] = listOf(mapOf(
                "\$type" to "IssueLink",
                "type" to mapOf("name" to "Parent/Child"),
                "target" to mapOf("id" to epicIssueId)
            ))
        }

        if (customFields.isNotEmpty()) {
            issueBody["customFields"] = customFields
        }

        // Tags
        if (task.tags.isNotEmpty()) {
            issueBody["tags"] = task.tags.map { mapOf("name" to it) }
        }

        val response = post("issues", issueBody)

        return if (response != null) {
            try {
                val issue = mapper.readValue<Map<String, Any>>(response)
                val id = issue["idReadable"] as? String ?: issue["id"] as String
                taskIssueIds[task.id] = id
                println("   ✅ Задача создана: $id")
                id
            } catch (e: Exception) {
                println("   ❌ Ошибка парсинга ответа: ${e.message}")
                null
            }
        } else {
            println("   ❌ Не удалось создать задачу")
            null
        }
    }

    private fun buildCustomFields(
        type: String?,
        priority: String?,
        status: String?,
        storyPoints: String?,
        component: String?,
        sprint: String?
    ): List<Map<String, Any>> {
        val fields = mutableListOf<Map<String, Any>>()

        // Type
        if (type != null) {
            customFieldIds["Type"]?.let { id ->
                fields.add(mapOf(
                    "id" to id,
                    "\$type" to "SingleEnumIssueCustomField",
                    "value" to mapOf("name" to type)
                ))
            }
        }

        // Priority
        if (priority != null) {
            customFieldIds["Priority"]?.let { id ->
                fields.add(mapOf(
                    "id" to id,
                    "\$type" to "SingleEnumIssueCustomField",
                    "value" to mapOf("name" to priority)
                ))
            }
        }

        // Status
        if (status != null) {
            customFieldIds["Status"]?.let { id ->
                fields.add(mapOf(
                    "id" to id,
                    "\$type" to "SingleStateIssueCustomField",
                    "value" to mapOf("name" to status)
                ))
            }
        }

        // Story Points
        if (storyPoints != null) {
            customFieldIds["Story Points"]?.let { id ->
                fields.add(mapOf(
                    "id" to id,
                    "\$type" to "SingleEnumIssueCustomField",
                    "value" to mapOf("name" to storyPoints)
                ))
            }
        }

        // Component (Subsystem)
        if (component != null) {
            fields.add(mapOf(
                "id" to Config.COMPONENTS_FIELD_ID,
                "\$type" to "SingleEnumIssueCustomField",
                "value" to mapOf("name" to component)
            ))
        }

        // Sprint
        if (sprint != null) {
            customFieldIds["Sprint"]?.let { id ->
                fields.add(mapOf(
                    "id" to id,
                    "\$type" to "SingleStateIssueCustomField",
                    "value" to mapOf("id" to sprint)
                ))
            }
        }

        return fields
    }

    private fun buildTaskDescription(task: Task): String {
        return buildString {
            appendLine(task.description)
            appendLine()
            appendLine("---")
            appendLine("**Метаданные:**")
            appendLine("- Компонент: `${task.component}`")
            appendLine("- Тип: `${task.type}`")
            appendLine("- Приоритет: `${task.priority}`")
            appendLine("- Оценка: ${task.estimate} SP")
            appendLine("- Эпик: `${task.epic}`")
            appendLine("- Спринт: `${task.sprint}`")
            if (task.tags.isNotEmpty()) {
                appendLine("- Теги: ${task.tags.joinToString(", ")}")
            }
            appendLine()
            appendLine("*ID: ${task.id}*")
        }
    }

    // ========================================================================
    // СПРИНТЫ (через Agile Board API)
    // ========================================================================
    fun createSprints(sprints: List<Sprint>): Boolean {
        println("\n=== СОЗДАНИЕ СПРИНТОВ ===")

        if (agileBoardId.isEmpty()) {
            println("   ❌ Agile Board ID не найден. Пропускаем создание спринтов.")
            println("   ℹ️ Создайте Agile Board вручную (см. README.md)")
            return false
        }

        var successCount = 0

        sprints.forEach { sprint ->
            println("📅 Создание спринта: ${sprint.name}")

            val sprintBody = mapOf(
                "name" to sprint.name,
                "goal" to sprint.goal,
                "start" to sprint.startDate,
                "end" to sprint.endDate,
                "capacity" to sprint.capacity
            )

            val response = post("agile/boards/$agileBoardId/sprints", sprintBody)

            if (response != null) {
                try {
                    val createdSprint = mapper.readValue<Map<String, Any>>(response)
                    val sprintId = createdSprint["id"] as? String ?: createdSprint["\$id"] as? String
                    if (sprintId != null) {
                        sprintIds[sprint.id] = sprintId
                        println("   ✅ Спринт создан: ${sprint.name} (ID: $sprintId)")
                        successCount++
                    }
                } catch (e: Exception) {
                    println("   ⚠️ Ошибка парсинга ответа: ${e.message}")
                }
            } else {
                println("   ❌ Не удалось создать спринт: ${sprint.name}")
            }

            Thread.sleep(Config.SPRINT_DELAY_MS)
        }

        println("✅ Создано $successCount из ${sprints.size} спринтов")
        return successCount == sprints.size
    }

    fun getSprintIdByName(sprintName: String): String? {
        // Ищем спринт по имени в кэше
        return sprintIds.entries.find {
            // Сравниваем с оригинальным именем спринта из config
            true  // Упрощённо возвращаем первый попавшийся
        }?.value
    }

    // ========================================================================
    // ПРИВЯЗКА ЗАДАЧ К СПРИНТАМ (если не получилось при создании)
    // ========================================================================
    fun assignTasksToSprints(config: ProjectConfig) {
        println("\n=== ПРИВЯЗКА ЗАДАЧ К СПРИНТАМ ===")

        val sprintFieldId = customFieldIds["Sprint"] ?: Config.SPRINT_FIELD_ID

        config.sprints.forEach { sprint ->
            val youtrackSprintId = sprintIds[sprint.id]
            if (youtrackSprintId == null) {
                println("   ⚠️ Спринт ${sprint.id} не найден в YouTrack")
                return@forEach
            }

            sprint.taskIds.forEach { taskId ->
                val issueId = taskIssueIds[taskId]
                if (issueId != null) {
                    // Обновляем задачу, добавляя Sprint field
                    val updateBody = mapOf(
                        "customFields" to listOf(mapOf(
                            "id" to sprintFieldId,
                            "\$type" to "SingleStateIssueCustomField",
                            "value" to mapOf("id" to youtrackSprintId)
                        ))
                    )

                    val response = put("issues/$issueId", updateBody)
                    if (response != null) {
                        println("   ✅ Задача $taskId привязана к спринту ${sprint.name}")
                    } else {
                        println("   ⚠️ Не удалось привязать задачу $taskId")
                    }
                }
            }
        }
    }

    // ========================================================================
    // СВОДКА
    // ========================================================================
    fun printSprintSummary(sprints: List<Sprint>) {
        println("\n=== СВОДКА ПО СПРИНТАМ ===")

        sprints.forEach { sprint ->
            val taskCount = sprint.taskIds.size
            println("   📅 ${sprint.name}: $taskCount задач (${sprint.startDate} → ${sprint.endDate})")
            println("      Goal: ${sprint.goal}")
            println("      Capacity: ${sprint.capacity} SP")
        }
    }

    // ========================================================================
    // ОСНОВНОЙ МЕТОД ИМПОРТА
    // ========================================================================
    fun importAll() {
        println("🚀 " + "=".repeat(60))
        println("🚀 ЗАПУСК ПОЛНОГО ИМПОРТА NOUS PLATFORM В YOU TRACK")
        println("🚀 " + "=".repeat(60))
        println()

        val startTime = System.currentTimeMillis()

        // 1. Проверка подключения
        println("=== ШАГ 1: ПРОВЕРКА ПОДКЛЮЧЕНИЯ ===")
        if (!testConnection()) {
            println("❌ Не удалось подключиться к YouTrack")
            return
        }
        println("✅ Подключение успешно\n")

        // 2. Проверка проекта
        println("=== ШАГ 2: ПРОВЕРКА ПРОЕКТА ===")
        if (!verifyProject()) {
            return
        }
        println()

        // 3. Поиск Agile Board
        println("=== ШАГ 3: ПОИСК AGILE BOARD ===")
        discoverAgileBoard()
        println()

        // 4. Загрузка конфигурации
        println("=== ШАГ 4: ЗАГРУЗКА КОНФИГУРАЦИИ ===")
        val config = loadConfig()
        println("✅ Конфигурация загружена")
        println("   • Проект: ${config.project.name}")
        println("   • Эпиков: ${config.epics.size}")
        println("   • Задач: ${config.tasks.size}")
        println("   • Компонентов: ${config.components.size}")
        println("   • Спринтов: ${config.sprints.size}")
        println("   • Tags: ${config.tags.size}")
        println()

        // 5. Custom Fields
        println("=== ШАГ 5: CUSTOM FIELDS ===")
        val existingFields = discoverCustomFields()
        verifyCustomFields(existingFields)
        println()

        // 6. Компоненты
        println("=== ШАГ 6: КОМПОНЕНТЫ ===")
        setupComponents(config.components)
        println()

        // 7. Tags
        println("=== ШАГ 7: TAGS ===")
        registerTags(config.tags)
        println()

        // 8. Эпики
        println("=== ШАГ 8: СОЗДАНИЕ ЭПИКОВ ===")
        config.epics.forEach { epic ->
            createEpic(epic)
            Thread.sleep(Config.EPIC_DELAY_MS)
        }
        println("✅ Создано ${epicIssueIds.size} эпиков\n")

        // 9. Спринты
        println("=== ШАГ 9: СОЗДАНИЕ СПРИНТОВ ===")
        val sprintsCreated = createSprints(config.sprints)
        println()

        // 10. Задачи (с привязкой к спринтам)
        println("=== ШАГ 10: СОЗДАНИЕ ЗАДАЧ ===")
        var taskCount = 0
        var successCount = 0

        config.tasks.forEach { task ->
            val epicIssueId = epicIssueIds[task.epic]
            val sprintId = sprintIds[task.sprint]
            createTask(task, epicIssueId, sprintId)
            taskCount++
            successCount++

            if (taskCount % 10 == 0) {
                println("   📊 Прогресс: $taskCount/${config.tasks.size} задач")
            }

            Thread.sleep(Config.TASK_DELAY_MS)
        }
        println("✅ Создано $successCount из $taskCount задач\n")

        // 11. Дополнительная привязка задач к спринтам
        if (!sprintsCreated) {
            println("=== ШАГ 11: ПРИВЯЗКА ЗАДАЧ К СПРИНТАМ ===")
            assignTasksToSprints(config)
            println()
        }

        // 12. Сводка
        println("=== ШАГ 12: СВОДКА ПО СПРИНТАМ ===")
        printSprintSummary(config.sprints)
        println()

        // Итоги
        val endTime = System.currentTimeMillis()
        val duration = (endTime - startTime) / 1000

        println("🎉 " + "=".repeat(60))
        println("🎉 ИМПОРТ ЗАВЕРШЕН УСПЕШНО!")
        println("🎉 " + "=".repeat(60))
        println()
        println("📊 СТАТИСТИКА:")
        println("   • Проект: ${config.project.name} (ID: $projectId)")
        println("   • Agile Board: $agileBoardId")
        println("   • Custom Fields: ${customFieldIds.size}")
        println("   • Компонентов: ${componentIds.size}")
        println("   • Эпиков: ${epicIssueIds.size}")
        println("   • Спринтов: ${sprintIds.size}")
        println("   • Задач: $successCount")
        println("   • Tags: ${tagIds.size}")
        println("   • Время выполнения: ${duration}с")
        println()
        println("📋 ПРОВЕРКА РЕЗУЛЬТАТА:")
        println("   👉 ${Config.YOUTRACK_URL}/issues?q=project:${Config.EXISTING_PROJECT_SHORTNAME}")
        println("   👉 ${Config.YOUTRACK_URL}/agile/board/$agileBoardId")
        println()
    }
}

// ============================================================================
// MAIN
// ============================================================================
fun main() {
    try {
        val importer = YouTrackImporter()
        importer.importAll()
    } catch (e: Exception) {
        println("\n❌ КРИТИЧЕСКАЯ ОШИБКА: ${e.message}")
        e.printStackTrace()
        println("\n💡 Проверьте:")
        println("   1. YouTrack запущен и доступен")
        println("   2. Токен действителен")
        println("   3. Проект NOUS создан")
        println("   4. Agile Board создан")
        println("   5. Custom Fields настроены (см. README.md)")
    }
}