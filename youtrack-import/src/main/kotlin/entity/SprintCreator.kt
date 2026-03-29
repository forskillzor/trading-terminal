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
        val startDateMillis = parseDateToMillis(sprint.startDate)
        val endDateMillis = parseDateToMillis(sprint.endDate)

        val body = mapOf(
            "name" to sprint.name,
            "goal" to sprint.goal,
            "start" to startDateMillis,
            "finish" to endDateMillis,
            "archived" to false
        )

        val response = httpClient.post("agiles/${Config.AGILE_BOARD_ID}/sprints", body)
        if (response == null) {
            println("❌ Не удалось создать спринт: ${sprint.name}")
            return null
        }

        try {
            val sprintNode = mapper.readTree(response)
            val idNode = sprintNode["id"]
            if (idNode != null) {
                val sprintId = idNode.asText()
                println("✅ Создан спринт: $sprintId")
                delay(Config.SPRINT_DELAY_MS)
                return sprintId
            } else {
                println("❌ Ответ не содержит id: $response")
                return null
            }
        } catch (e: Exception) {
            println("❌ Ошибка при разборе ответа: ${e.message}")
            return null
        }
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
    private fun parseDateToMillis(dateString: String): Long {
        return try {
            val format = java.time.format.DateTimeFormatter.ISO_LOCAL_DATE
            val localDate = java.time.LocalDate.parse(dateString, format)
            localDate.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
        } catch (e: Exception) {
            println("⚠️ Ошибка парсинга даты $dateString: ${e.message}")
            System.currentTimeMillis()
        }
    }
}