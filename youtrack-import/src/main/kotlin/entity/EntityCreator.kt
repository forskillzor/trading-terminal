package entity

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import http.HttpClient
import config.Config
import models.FieldInfo
import kotlinx.coroutines.delay

abstract class EntityCreator(
    protected val httpClient: HttpClient,
    protected val fields: Map<String, FieldInfo>,
    protected val componentsFieldId: String?
) {
    protected val mapper = ObjectMapper().apply {
        registerModule(KotlinModule.Builder().build())
        configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
    }

    protected suspend fun delay(ms: Long) {
        kotlinx.coroutines.delay(ms)
    }

    protected fun buildCustomFields(customFields: Map<String, Any?>): List<Map<String, Any>> {
        val result = mutableListOf<Map<String, Any>>()
        customFields.forEach { (fieldName, value) ->
            val fieldInfo = fields[fieldName]
            if (fieldInfo != null && value != null) {
                println("   🛠️  Построение поля '$fieldName' (тип: ${fieldInfo.fieldType}, значение: $value)")
                val fieldMap = mutableMapOf<String, Any>()
                fieldMap["id"] = fieldInfo.id
                fieldMap["\$type"] = "SingleCustomField"

                when {
                    // State поля (Status, Sprint)
                    fieldInfo.fieldType.startsWith("state") -> {
                        fieldMap["value"] = mapOf(
                            "name" to value.toString(),
                            "\$type" to "StateIssueCustomFieldElement"
                        )
                    }
                    // Enum поля (Priority, Type, Story Points)
                    fieldInfo.fieldType.startsWith("enum") -> {
                        if (fieldInfo.fieldType == "enum[*]" && value is List<*>) {
                            // Multi-select (Components)
                            fieldMap["value"] = value.map { item ->
                                mapOf(
                                    "name" to item.toString(),
                                    "\$type" to "EnumIssueCustomFieldElement"
                                )
                            }
                            fieldMap["\$type"] = "MultiEnumIssueCustomField"
                        } else {
                            // Single enum
                            fieldMap["value"] = mapOf(
                                "name" to value.toString(),
                                "\$type" to "EnumIssueCustomFieldElement"
                            )
                        }
                    }
                    // User поля (Assignee)
                    fieldInfo.fieldType.startsWith("user") -> {
                        fieldMap["value"] = mapOf(
                            "login" to value,
                            "\$type" to "User"
                        )
                    }
                    // Epic field - это ссылка на задачу, а не enum!
                    fieldName == "Epic" -> {
                        fieldMap["value"] = mapOf(
                            "id" to value,
                            "\$type" to "Issue"
                        )
                    }

                    else -> {
                        fieldMap["value"] = value
                    }
                }
                result.add(fieldMap)
            } else if (value != null) {
                println("   ⚠️  Поле '$fieldName' не найдено в списке полей, пропускаем")
            }
        }
        println("   📋 Построено ${result.size} кастомных полей")
        return result
    }

    protected fun buildTaskDescription(task: models.Task): String {
        return buildString {
            append("**Описание:** ${task.description}\n\n")

            if (task.component.isNotEmpty()) {
                append("**Компонент:** ${task.component}\n\n")
            }

            if (task.estimate > 0) {
                append("**Оценка:** ${task.estimate} story points\n\n")
            }

            if (task.tags.isNotEmpty()) {
                append("**Теги:** ${task.tags.joinToString(", ")}\n\n")
            }

            if (task.acceptanceCriteria.isNotEmpty()) {
                append("**Критерии приемки:**\n")
                task.acceptanceCriteria.forEach { append("- $it\n") }
                append("\n")
            }
            if (task.notes.isNotEmpty()) {
                append("**Заметки:**\n")
                task.notes.forEach { append("- $it\n") }
                append("\n")
            }
            if (task.relatedFiles.isNotEmpty()) {
                append("**Связанные файлы:**\n")
                task.relatedFiles.forEach { append("- $it\n") }
                append("\n")
            }
            if (task.dependencies.isNotEmpty()) {
                append("**Зависимости:**\n")
                task.dependencies.forEach { append("- $it\n") }
                append("\n")
            }
        }
    }
}