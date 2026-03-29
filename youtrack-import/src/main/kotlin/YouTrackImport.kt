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

// Конфигурация
object Config {
    val YOUTRACK_URL = "http://localhost:8081"
    val TOKEN = "perm-YWRtaW4=.NDItMQ==.LLbeWWAZFVOXicroCXshF8D2sPvAm8"
    val VERBOSE = true
    val PROJECT_JSON_PATH = "youtrack-import/src/main/kotlin/project.json"
    val EXISTING_PROJECT_ID = "0-8" // Существующий проект Nous
    val EXISTING_PROJECT_SHORTNAME = "NOUS"
    val COMPONENTS_FIELD_ID = "177-54" // Поле "Subsystem" для компонентов
    val COMPONENTS_BUNDLE_ID = "152-0" // Bundle для компонентов
}

// Модели данных из project.json
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

// Основной класс импортера
class YouTrackImporter {
    private val logging = HttpLoggingInterceptor().apply {
        level = if (Config.VERBOSE) HttpLoggingInterceptor.Level.BASIC else HttpLoggingInterceptor.Level.NONE
    }

    private val client = OkHttpClient.Builder()
        .addInterceptor(logging)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val mapper = ObjectMapper().apply {
        registerModule(KotlinModule.Builder().build())
        configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
    }
    private val JSON = "application/json; charset=utf-8".toMediaType()

    private var projectId: String? = null
    private var customFieldIds = mutableMapOf<String, String>() // name -> id
    private var componentIds = mutableMapOf<String, String>() // component name -> bundle id
    private var epicIds = mutableMapOf<String, String>() // epic id -> issue id

    // Загрузка конфигурации
    fun loadConfig(): ProjectConfig {
        println("📂 Загрузка конфигурации из ${Config.PROJECT_JSON_PATH}")
        // Пробуем несколько возможных путей
        val possiblePaths = listOf(
            Config.PROJECT_JSON_PATH,
            "../${Config.PROJECT_JSON_PATH}",
            "../../${Config.PROJECT_JSON_PATH}",
            "src/main/kotlin/project.json"
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
            throw RuntimeException("Файл project.json не найден. Искали по путям: ${possiblePaths}")
        }
        
        val json = file.readText()
        return mapper.readValue(json)
    }

    // Создание HTTP запроса
    private fun createRequest(url: String, method: String = "GET", body: String? = null): Request {
        val builder = Request.Builder()
            .url("${Config.YOUTRACK_URL}/api/$url")
            .addHeader("Accept", "application/json")
            .addHeader("Content-Type", "application/json")
            .addHeader("Authorization", "Bearer ${Config.TOKEN}")

        if (body != null) {
            builder.method(method, body.toRequestBody(JSON))
        } else {
            builder.method(method, null)
        }

        return builder.build()
    }

    // Выполнение запроса
    private fun executeRequest(request: Request): String? {
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

    // Проверка подключения
    fun testConnection(): Boolean {
        println("🔌 Проверка подключения к YouTrack...")
        val request = createRequest("admin/projects")
        return executeRequest(request) != null
    }

    // Проверка существующих полей в проекте
    fun checkExistingFields(): Map<String, String> {
        println("🔍 Проверка существующих полей в проекте...")
        val existingFields = mutableMapOf<String, String>()
        
        val request = createRequest("admin/projects/${Config.EXISTING_PROJECT_ID}/customFields?fields=id,name")
        val response = executeRequest(request)
        
        if (response != null) {
            try {
                val fields = mapper.readValue<List<Map<String, Any>>>(response)
                fields.forEach { field ->
                    val id = field["id"] as? String
                    val name = field["name"] as? String
                    if (id != null && name != null) {
                        existingFields[name] = id
                        println("   📋 Найдено поле: $name (ID: $id)")
                    }
                }
            } catch (e: Exception) {
                println("   ⚠️  Ошибка при парсинге полей: ${e.message}")
            }
        }
        
        return existingFields
    }

    // Создание проекта
    fun createProject(projectInfo: ProjectInfo): String? {
        println("🏗️  Создание проекта: ${projectInfo.name}")
        
        val projectBody = mapOf(
            "name" to projectInfo.name,
            "shortName" to projectInfo.id,
            "description" to projectInfo.description,
            "leader" to mapOf("login" to projectInfo.lead)
        )
        
        val request = createRequest("admin/projects", "POST", mapper.writeValueAsString(projectBody))
        val response = executeRequest(request)
        
        return if (response != null) {
            val project = mapper.readValue<Map<String, Any>>(response)
            val id = project["id"] as String
            projectId = id
            println("   ✅ Проект создан: $id")
            id
        } else {
            println("❌ Не удалось создать проект")
            null
        }
    }

    // Создание bundle для enum значений
    fun createBundle(name: String, values: List<String>): String? {
        println("📦 Создание bundle: $name")
        
        val bundleValues = values.map { mapOf("name" to it) }
        val bundleBody = mapOf(
            "name" to name,
            "values" to bundleValues
        )
        
        val request = createRequest("admin/bundles", "POST", mapper.writeValueAsString(bundleBody))
        val response = executeRequest(request)
        
        return if (response != null) {
            val bundle = mapper.readValue<Map<String, Any>>(response)
            val id = bundle["id"] as String
            println("   ✅ Bundle создан: $id")
            id
        } else {
            println("❌ Не удалось создать bundle")
            null
        }
    }

    // Создание кастомного поля
    fun createCustomField(fieldName: String, config: CustomFieldConfig): String? {
        println("🔧 Создание поля: $fieldName (${config.type})")
        
        if (projectId == null) {
            println("❌ Проект не создан")
            return null
        }
        
        val fieldType = when (config.type) {
            "enum" -> "enum[1]"
            "state" -> "state[1]"
            "period" -> "period"
            else -> config.type
        }
        
        val fieldBody = mutableMapOf<String, Any>(
            "name" to fieldName,
            "fieldType" to mapOf("id" to fieldType)
        )
        
        // Для enum полей нужен bundle
        if (config.type == "enum" && config.values != null) {
            val bundleId = createBundle("$fieldName Values", config.values)
            if (bundleId != null) {
                fieldBody["bundle"] = mapOf("id" to bundleId)
            }
        }
        
        val request = createRequest("admin/projects/$projectId/customFields", "POST", mapper.writeValueAsString(fieldBody))
        val response = executeRequest(request)
        
        return if (response != null) {
            val field = mapper.readValue<Map<String, Any>>(response)
            val id = field["id"] as String
            customFieldIds[fieldName] = id
            println("   ✅ Поле создано: $id")
            id
        } else {
            println("❌ Не удалось создать поле")
            null
        }
    }

    // Создание компонентов (как bundle)
    fun createComponents(components: List<Component>) {
        println("🧩 Создание компонентов")
        
        val componentNames = components.map { it.name }
        val bundleId = createBundle("Components", componentNames)
        
        if (bundleId != null) {
            // Сохраняем mapping компонентов
            components.forEach { component ->
                componentIds[component.name] = bundleId
            }
            println("   ✅ Создано ${components.size} компонентов")
        }
    }

    // Создание эпика
    fun createEpic(epic: Epic): String? {
        println("🎯 Создание эпика: ${epic.summary}")
        
        if (projectId == null) {
            println("❌ Проект не создан")
            return null
        }
        
        val customFields = mutableListOf<Map<String, Any>>()
        
        // Добавляем поле Type если есть
        val typeFieldId = customFieldIds["Type"]
        if (typeFieldId != null) {
            customFields.add(mapOf(
                "id" to typeFieldId,
                "\$type" to "SingleEnumIssueCustomField",
                "value" to mapOf("name" to "Epic")
            ))
        }
        
        // Добавляем поле Priority
        val priorityFieldId = customFieldIds["Priority"]
        if (priorityFieldId != null) {
            customFields.add(mapOf(
                "id" to priorityFieldId,
                "\$type" to "SingleEnumIssueCustomField",
                "value" to mapOf("name" to epic.priority)
            ))
        }
        
        val issueBody = mutableMapOf<String, Any>(
            "project" to mapOf("id" to projectId),
            "summary" to epic.summary,
            "description" to epic.description
        )
        
        if (customFields.isNotEmpty()) {
            issueBody["customFields"] = customFields
        }
        
        val request = createRequest("issues", "POST", mapper.writeValueAsString(issueBody))
        val response = executeRequest(request)
        
        return if (response != null) {
            val issue = mapper.readValue<Map<String, Any>>(response)
            val id = issue["id"] as String
            epicIds[epic.id] = id
            println("   ✅ Эпик создан: $id")
            id
        } else {
            println("❌ Не удалось создать эпик")
            null
        }
    }

    // Создание задачи
    fun createTask(task: Task): String? {
        println("📝 Создание задачи: ${task.summary}")
        
        if (projectId == null) {
            println("❌ Проект не создан")
            return null
        }
        
        val customFields = mutableListOf<Map<String, Any>>()
        
        // Добавляем поле Type
        val typeFieldId = customFieldIds["Type"]
        if (typeFieldId != null) {
            customFields.add(mapOf(
                "id" to typeFieldId,
                "\$type" to "SingleEnumIssueCustomField",
                "value" to mapOf("name" to task.type)
            ))
        }
        
        // Добавляем поле Priority
        val priorityFieldId = customFieldIds["Priority"]
        if (priorityFieldId != null) {
            customFields.add(mapOf(
                "id" to priorityFieldId,
                "\$type" to "SingleEnumIssueCustomField",
                "value" to mapOf("name" to task.priority)
            ))
        }
        
        // Добавляем поле Story Points
        val storyPointsFieldId = customFieldIds["Story Points"]
        if (storyPointsFieldId != null && task.estimate > 0) {
            customFields.add(mapOf(
                "id" to storyPointsFieldId,
                "\$type" to "SingleEnumIssueCustomField",
                "value" to mapOf("name" to task.estimate.toString())
            ))
        }
        
        // Добавляем компонент (Subsystem поле)
        if (task.component.isNotEmpty()) {
            customFields.add(mapOf(
                "id" to Config.COMPONENTS_FIELD_ID,
                "\$type" to "SingleEnumIssueCustomField",
                "value" to mapOf("name" to task.component)
            ))
        }
        
        val issueBody = mutableMapOf<String, Any>(
            "project" to mapOf("id" to projectId),
            "summary" to task.summary,
            "description" to task.description
        )
        
        if (customFields.isNotEmpty()) {
            issueBody["customFields"] = customFields
        }
        
        val request = createRequest("issues", "POST", mapper.writeValueAsString(issueBody))
        val response = executeRequest(request)
        
        return if (response != null) {
            val issue = mapper.readValue<Map<String, Any>>(response)
            val id = issue["idReadable"] as? String ?: issue["id"] as String
            println("   ✅ Задача создана: $id (компонент: ${task.component})")
            id
        } else {
            println("❌ Не удалось создать задачу")
            null
        }
    }

    // Основной метод импорта
    fun importAll() {
        println("🚀 ЗАПУСК ПОЛНОГО ИМПОРТА В YOU TRACK")
        println("========================================")
        
        // 1. Проверка подключения
        if (!testConnection()) {
            println("❌ Не удалось подключиться к YouTrack")
            return
        }
        println("✅ Подключение успешно")
        
        // 2. Загрузка конфигурации
        val config = loadConfig()
        println("✅ Конфигурация загружена")
        println("   • Проект: ${config.project.name}")
        println("   • Эпиков: ${config.epics.size}")
        println("   • Задач: ${config.tasks.size}")
        println("   • Компонентов: ${config.components.size}")
        
        // 3. Используем существующий проект
        projectId = Config.EXISTING_PROJECT_ID
        println("📊 Используем существующий проект: $projectId")
        
        // 4. Проверка существующих полей
        println("\n=== ПРОВЕРКА СУЩЕСТВУЮЩИХ ПОЛЕЙ ===")
        val existingFields = checkExistingFields()
        
        // 5. Сопоставление полей из конфигурации с существующими
        println("\n=== СОПОСТАВЛЕНИЕ ПОЛЕЙ ===")
        config.customFields.forEach { (fieldName, fieldConfig) ->
            val existingFieldId = existingFields[fieldName]
            if (existingFieldId != null) {
                customFieldIds[fieldName] = existingFieldId
                println("   ✅ Используем существующее поле: $fieldName (ID: $existingFieldId)")
            } else {
                println("   ⚠️  Поле '$fieldName' не найдено в проекте")
                println("   ℹ️  Создайте поле вручную через UI или используйте другое имя")
            }
        }
        
        // Проверяем поле Components (Subsystem)
        if (existingFields.any { it.key.contains("Subsystem", ignoreCase = true) }) {
            println("   ✅ Поле Components (Subsystem) найдено")
        } else {
            println("   ⚠️  Поле Components не найдено. Используем ID: ${Config.COMPONENTS_FIELD_ID}")
        }
        
        // 6. Создание эпиков
        println("\n=== СОЗДАНИЕ ЭПИКОВ ===")
        config.epics.forEach { epic ->
            createEpic(epic)
            Thread.sleep(500)
        }
        
        // 7. Создание задач
        println("\n=== СОЗДАНИЕ ЗАДАЧ ===")
        var taskCount = 0
        config.tasks.forEach { task ->
            createTask(task)
            taskCount++
            
            // Прогресс каждые 10 задач
            if (taskCount % 10 == 0) {
                println("   📊 Прогресс: создано $taskCount из ${config.tasks.size} задач")
            }
            
            Thread.sleep(300)
        }
        
        println("\n" + "=".repeat(50))
        println("✅ ПОЛНЫЙ ИМПОРТ ЗАВЕРШЕН УСПЕШНО!")
        println("📊 Статистика:")
        println("   • Проект: ${config.project.name} (ID: $projectId)")
        println("   • Использовано полей: ${customFieldIds.size}")
        println("   • Эпиков: ${epicIds.size}")
        println("   • Задач: $taskCount")
        println("\n📋 Проверьте проект в YouTrack")
        println("👉 Откройте: ${Config.YOUTRACK_URL}/issues?q=project:${Config.EXISTING_PROJECT_SHORTNAME}")
    }
}

fun main() {
    val importer = YouTrackImporter()
    importer.importAll()
}