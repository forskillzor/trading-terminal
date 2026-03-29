package field

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import http.HttpClient
import config.Config
import models.FieldInfo

class FieldDiscovery(private val httpClient: HttpClient) {
    private val mapper = ObjectMapper().apply {
        registerModule(KotlinModule.Builder().build())
        configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
    }

    fun discoverAllFields(): Map<String, FieldInfo> {
        println("🔍 Получение всех полей проекта...")
        val response = httpClient.get("admin/projects/${Config.EXISTING_PROJECT_ID}/customFields?fields=id,name,field(id,name,localizedName)")
        if (response == null) {
            println("❌ Не удалось получить список полей")
            return emptyMap()
        }

        try {
            val fields = mapper.readTree(response)
            val result = mutableMapOf<String, FieldInfo>()

            fields.forEach { field ->
                val idNode = field["id"]
                val nameNode = field["name"]
                val fieldNode = field["field"]
                
                if (idNode != null && nameNode != null && fieldNode != null) {
                    val id = idNode.asText()
                    val name = nameNode.asText()
                    val fieldTypeNode = fieldNode["id"]
                    val localizedNameNode = fieldNode["localizedName"]
                    
                    val fieldType = fieldTypeNode?.asText() ?: "unknown"
                    val localizedName = localizedNameNode?.asText() ?: name
                    
                    result[name] = FieldInfo(id, name, fieldType, localizedName)
                } else {
                    println("⚠️ Пропущено поле с неполными данными: $field")
                }
            }

            println("✅ Найдено ${result.size} полей:")
            result.values.forEach { println("   - ${it.name} (${it.id})") }
            return result
        } catch (e: Exception) {
            println("❌ Ошибка при разборе ответа: ${e.message}")
            e.printStackTrace()
            return emptyMap()
        }
    }

    fun discoverComponentsFieldId(fields: Map<String, FieldInfo>): String? {
        val componentsField = fields.values.find { it.name == "Components" }
        if (componentsField != null) {
            println("✅ Найдено поле Components: ${componentsField.id}")
            return componentsField.id
        }

        println("⚠️ Поле Components не найдено в списке полей, пытаемся найти через API...")
        val response = httpClient.get("admin/projects/${Config.EXISTING_PROJECT_ID}/customFields?fields=id,name,field(id,name,localizedName)")
        if (response == null) {
            println("❌ Не удалось получить поля для поиска Components")
            return null
        }

        try {
            val allFields = mapper.readTree(response)
            for (field in allFields) {
                val nameNode = field["name"]
                if (nameNode != null && nameNode.asText() == "Components") {
                    val idNode = field["id"]
                    if (idNode != null) {
                        val id = idNode.asText()
                        println("✅ Найдено поле Components: $id")
                        return id
                    }
                }
            }
        } catch (e: Exception) {
            println("❌ Ошибка при поиске поля Components: ${e.message}")
        }

        println("❌ Поле Components не найдено")
        return null
    }

    fun verifyRequiredFields(fields: Map<String, FieldInfo>): Boolean {
        val required = listOf("Epic", "Sprint", "Priority", "Type", "State", "Assignee")
        val missing = mutableListOf<String>()

        required.forEach { fieldName ->
            if (!fields.containsKey(fieldName)) {
                missing.add(fieldName)
            }
        }

        if (missing.isNotEmpty()) {
            println("❌ Отсутствуют обязательные поля: ${missing.joinToString(", ")}")
            println("   Убедитесь, что эти поля созданы в проекте YouTrack")
            return false
        }

        println("✅ Все обязательные поля присутствуют")
        return true
    }
}