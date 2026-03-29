package entity

import http.HttpClient
import models.Task
import models.FieldInfo
import config.Config

class TaskCreator(
    httpClient: HttpClient,
    fields: Map<String, FieldInfo>,
    componentsFieldId: String?
) : EntityCreator(httpClient, fields, componentsFieldId) {

    suspend fun createTask(task: Task, epicId: String? = null): String? {
        println("📝 Создание задачи: ${task.summary}")

        val customFields = mutableMapOf<String, Any?>(
            "Priority" to task.priority,
            "Type" to task.type,
            "Status" to "To Do"
        )

        if (epicId != null) {
            customFields["Epic"] = epicId
        }

        if (task.assignee.isNotEmpty()) {
            customFields["Assignee"] = task.assignee
        }

        if (task.estimate > 0) {
            customFields["Story Points"] = task.estimate.toString()
        }

        if (task.sprint.isNotEmpty()) {
            customFields["Sprint"] = task.sprint
        }

        val body = mutableMapOf<String, Any>(
            "project" to mapOf("id" to Config.EXISTING_PROJECT_ID),
            "summary" to task.summary,
            "description" to buildTaskDescription(task),
            "customFields" to buildCustomFields(customFields)
        )

        // Добавляем компоненты, если они указаны
        if (task.component.isNotEmpty()) {
            body["components"] = listOf(
                mapOf("name" to task.component)
            )
        }

        val response = httpClient.post("issues", body)
        if (response == null) {
            println("❌ Не удалось создать задачу: ${task.summary}")
            return null
        }

        try {
            val issueNode = mapper.readTree(response)
            val idReadableNode = issueNode["idReadable"]
            if (idReadableNode != null) {
                val issueId = idReadableNode.asText()
                println("✅ Создана задача: $issueId")
                delay(Config.TASK_DELAY_MS)
                return issueId
            } else {
                println("❌ Ответ не содержит idReadable: $response")
                return null
            }
        } catch (e: Exception) {
            println("❌ Ошибка при разборе ответа: ${e.message}")
            return null
        }
    }
}