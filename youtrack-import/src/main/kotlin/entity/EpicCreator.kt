package entity

import http.HttpClient
import models.Epic
import models.FieldInfo
import config.Config

class EpicCreator(
    httpClient: HttpClient,
    fields: Map<String, FieldInfo>,
    componentsFieldId: String?
) : EntityCreator(httpClient, fields, componentsFieldId) {

    suspend fun createEpic(epic: Epic): String? {
        println("📦 Создание эпика: ${epic.summary}")

        val customFields = mutableMapOf<String, Any?>(
            "Priority" to epic.priority,
            "Type" to "Epic",
            "Status" to "To Do"
        )

        if (epic.assignee.isNotEmpty()) {
            customFields["Assignee"] = epic.assignee
        }

        val body = mapOf(
            "project" to mapOf("id" to Config.EXISTING_PROJECT_ID),
            "summary" to epic.summary,
            "description" to epic.description,
            "customFields" to buildCustomFields(customFields)
        )

        val response = httpClient.post("issues", body)
        if (response == null) {
            println("❌ Не удалось создать эпик: ${epic.summary}")
            return null
        }

        try {
            val issueNode = mapper.readTree(response)
            val idReadableNode = issueNode["idReadable"]
            if (idReadableNode != null) {
                val issueId = idReadableNode.asText()
                println("✅ Создан эпик: $issueId")
                delay(Config.EPIC_DELAY_MS)
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