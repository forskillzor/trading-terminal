package entity

import http.HttpClient
import models.Sprint
import models.FieldInfo
import config.Config

class SprintCreator(
    httpClient: HttpClient,
    fields: Map<String, FieldInfo>,
    componentsFieldId: String?
) : EntityCreator(httpClient, fields, componentsFieldId) {

    suspend fun createSprint(sprint: Sprint): String? {
        println("🏃 Создание спринта: ${sprint.name}")

        val body = mapOf(
            "name" to sprint.name,
            "goal" to sprint.goal,
            "start" to sprint.startDate,
            "finish" to sprint.endDate,
            "archived" to false
        )

        val response = httpClient.post("agiles/${Config.AGILE_BOARD_ID}/sprints", body)
        if (response == null) {
            println("❌ Не удалось создать спринт: ${sprint.name}")
            return null
        }

        val sprintNode = mapper.readTree(response)
        val sprintId = sprintNode["id"].asText()
        println("✅ Создан спринт: $sprintId")

        delay(Config.SPRINT_DELAY_MS)
        return sprintId
    }

    suspend fun addIssueToSprint(sprintId: String, issueId: String): Boolean {
        val body = mapOf(
            "issues" to listOf(mapOf("id" to issueId))
        )

        val response = httpClient.post("agiles/${Config.AGILE_BOARD_ID}/sprints/$sprintId/issues", body)
        if (response == null) {
            println("❌ Не удалось добавить задачу $issueId в спринт $sprintId")
            return false
        }

        println("✅ Задача $issueId добавлена в спринт $sprintId")
        return true
    }
}