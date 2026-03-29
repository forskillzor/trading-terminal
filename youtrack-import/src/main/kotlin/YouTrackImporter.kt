package youtrackimport

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import config.Config
import field.FieldDiscovery
import http.HttpClient
import entity.EpicCreator
import entity.TaskCreator
import entity.SprintCreator
import models.ProjectConfig
import models.FieldInfo
import kotlinx.coroutines.runBlocking

class YouTrackImporter {
    private val httpClient = HttpClient()
    private val fieldDiscovery = FieldDiscovery(httpClient)
    private val mapper = ObjectMapper().apply {
        registerModule(KotlinModule.Builder().build())
        configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
    }

    private lateinit var fields: Map<String, FieldInfo>
    private var componentsFieldId: String? = null
    private lateinit var epicCreator: EpicCreator
    private lateinit var taskCreator: TaskCreator
    private lateinit var sprintCreator: SprintCreator

    private fun loadProjectConfig(): ProjectConfig {
        println("📄 Загрузка конфигурации проекта из ${Config.PROJECT_JSON_PATH}")
        try {
            // Используем путь относительно текущей директории
            val file = java.io.File(Config.PROJECT_JSON_PATH)
            if (!file.exists()) {
                // Пробуем найти файл в разных местах
                val alternativePaths = listOf(
                    "project.json",
                    "src/main/kotlin/project.json",
                    "youtrack-import/src/main/kotlin/project.json"
                )
                
                for (path in alternativePaths) {
                    val altFile = java.io.File(path)
                    if (altFile.exists()) {
                        println("   Найден файл по альтернативному пути: $path")
                        val json = altFile.readText()
                        return mapper.readValue(json, ProjectConfig::class.java)
                    }
                }
                throw java.io.FileNotFoundException("Файл не найден: ${Config.PROJECT_JSON_PATH}. Проверенные пути: ${alternativePaths.joinToString(", ")}")
            }
            
            val json = file.readText()
            return mapper.readValue(json, ProjectConfig::class.java)
        } catch (e: Exception) {
            println("❌ Ошибка загрузки конфигурации: ${e.message}")
            throw e
        }
    }

    private fun initializeCreators() {
        epicCreator = EpicCreator(httpClient, fields, componentsFieldId)
        taskCreator = TaskCreator(httpClient, fields, componentsFieldId)
        sprintCreator = SprintCreator(httpClient, fields, componentsFieldId)
    }

    private suspend fun discoverFields() {
        fields = fieldDiscovery.discoverAllFields()
        if (fields.isEmpty()) {
            throw IllegalStateException("Не удалось получить поля проекта")
        }

        if (!fieldDiscovery.verifyRequiredFields(fields)) {
            throw IllegalStateException("Отсутствуют обязательные поля")
        }

        componentsFieldId = fieldDiscovery.discoverComponentsFieldId(fields)
        initializeCreators()
    }

    private suspend fun createEpics(config: ProjectConfig): Map<String, String> {
        println("\n🎯 Создание эпиков (${config.epics.size} шт.)")
        val epicMap = mutableMapOf<String, String>()

        for (epic in config.epics) {
            val epicId = epicCreator.createEpic(epic)
            if (epicId != null) {
                epicMap[epic.name] = epicId
            }
        }

        return epicMap
    }

    private suspend fun createTasks(config: ProjectConfig, epicMap: Map<String, String>) {
        println("\n📝 Создание задач (${config.tasks.size} шт.)")

        for (task in config.tasks) {
            val epicId = if (task.epic.isNotEmpty()) epicMap[task.epic] else null
            taskCreator.createTask(task, epicId)
        }
    }

    private suspend fun createSprints(config: ProjectConfig): Map<String, String> {
        println("\n🏃 Создание спринтов (${config.sprints.size} шт.)")
        val sprintMap = mutableMapOf<String, String>()

        for (sprint in config.sprints) {
            val sprintId = sprintCreator.createSprint(sprint)
            if (sprintId != null) {
                sprintMap[sprint.name] = sprintId
            }
        }

        return sprintMap
    }

    suspend fun importAll() {
        println("🚀 Запуск импорта в YouTrack")
        println("URL: ${Config.YOUTRACK_URL}")
        println("Проект: ${Config.EXISTING_PROJECT_SHORTNAME} (ID: ${Config.EXISTING_PROJECT_ID})")

        discoverFields()

        val config = loadProjectConfig()
        println("\n📊 Конфигурация проекта:")
        println("   - Эпиков: ${config.epics.size}")
        println("   - Задач: ${config.tasks.size}")
        println("   - Спринтов: ${config.sprints.size}")

        val epicMap = createEpics(config)
        createTasks(config, epicMap)
        createSprints(config)

        println("\n✅ Импорт завершен!")
    }
}

fun main() = runBlocking {
    val importer = YouTrackImporter()
    try {
        importer.importAll()
    } catch (e: Exception) {
        println("❌ Ошибка импорта: ${e.message}")
        e.printStackTrace()
    }
}