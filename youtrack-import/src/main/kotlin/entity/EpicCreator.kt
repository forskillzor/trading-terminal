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
        println("📦 Создание эпика: ${epic.name}")

        val customFields = mutableMapOf<String, Any?>(
            "Epic" to epic.name,
            "Priority" to epic.priority,
            "Type" to "Epic",
            "State" to "To Do"
        )

        if (epic.assignee.isNotEmpty()) {
            customFields["Assignee"] = epic.assignee
        }

        val body = mapOf(
            "project" to mapOf("id" to Config.EXISTING_PROJECT_ID),
            "summary" to epic.name,
            "description" to epic.description,
            "customFields" to buildCustomFields(customFields)
        )

        val response = httpClient.post("issues", body)
        if (response == null) {
            println("❌ Не удалось создать эпик: ${epic.name}")
            return null
        }

        val issueNode = mapper.readTree(response)
        val issueId = issueNode["idReadable"].asText()
        println("✅ Создан эпик: $issueId")

        delay(Config.EPIC_DELAY_MS)
        return issueId
    }
}