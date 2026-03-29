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
        println("📝 Создание задачи: ${task.name}")

        val customFields = mutableMapOf<String, Any?>(
            "Priority" to task.priority,
            "Type" to task.type,
            "State" to "To Do"
        )

        if (epicId != null) {
            customFields["Epic"] = epicId
        }

        if (task.assignee.isNotEmpty()) {
            customFields["Assignee"] = task.assignee
        }

        if (componentsFieldId != null && task.components.isNotEmpty()) {
            customFields["Components"] = task.components
        }

        val body = mapOf(
            "project" to mapOf("id" to Config.EXISTING_PROJECT_ID),
            "summary" to task.name,
            "description" to buildTaskDescription(task),
            "customFields" to buildCustomFields(customFields)
        )

        val response = httpClient.post("issues", body)
        if (response == null) {
            println("❌ Не удалось создать задачу: ${task.name}")
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