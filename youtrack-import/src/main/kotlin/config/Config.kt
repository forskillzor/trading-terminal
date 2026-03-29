package config

object Config {
    val YOUTRACK_URL = "http://localhost:8081"
    val TOKEN = "perm-YWRtaW4=.NDItMQ==.LLbeWWAZFVOXicroCXshF8D2sPvAm8"
    val VERBOSE = true
    val PROJECT_JSON_PATH = "src/main/kotlin/project.json"

    // Эти значения нужно получить после ручной настройки (см. README.md)
    val EXISTING_PROJECT_ID = "0-11"
    val EXISTING_PROJECT_SHORTNAME = "NOUS"
    val AGILE_BOARD_ID = "1"  // ID Agile Board (найти в UI)

    // Delays
    val EPIC_DELAY_MS = 500L
    val TASK_DELAY_MS = 300L
    val SPRINT_DELAY_MS = 400L
}