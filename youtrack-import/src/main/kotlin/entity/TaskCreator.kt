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

        val issueNode = mapper.readTree(response)
        val issueId = issueNode["idReadable"].asText()
        println("✅ Создана задача: $issueId")

        delay(Config.TASK_DELAY_MS)
        return issueId
    }
}