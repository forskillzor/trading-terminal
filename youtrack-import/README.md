# YouTrack Project Import - NOUS Platform

## 📋 Предварительная настройка (Manual Setup)

### Шаг 1: Создание проекта (5 минут)

1. Откройте YouTrack: `http://localhost:8081`
2. **Administration** → **Projects** → **Create Project**
3. Заполните:
    - **Name**: `Nous Platform`
    - **Short Name**: `NOUS`
    - **Description**: `Kotlin Multiplatform trading platform with plugin system`
    - **Lead**: `skillzor`
    - **Template**: `Scrum`
4. **Запишите Project ID** (например: `0-8`) → вставьте в `Config.EXISTING_PROJECT_ID`

### Шаг 2: Создание Custom Fields (10 минут)

**Administration** → **Projects** → **NOUS** → **Custom Fields**

| Field Name | Type | Values |
|------------|------|--------|
| `Story Points` | Enum | `1, 2, 3, 5, 8, 13, 21, 34, 55` |
| `Priority` | Enum | `Low, Medium, High, Critical` |
| `Type` | Enum | `Task, Feature, Bug, Epic, Spike, Improvement` |
| `Status` | State | `Backlog, To Do, In Progress, Code Review, Testing, Done` |
| `Sprint` | State | *(создаётся автоматически при создании спринтов)* |

**Важно**: Для `Status` и `Sprint` выберите тип **State** (не Enum)!

### Шаг 3: Создание Agile Board (5 минут) ⭐ КРИТИЧНО ДЛЯ СПРИНТОВ

1. Перейдите в **Agile** → **Create Board**
2. Name: `Nous Platform Board`
3. Project: `NOUS`
4. Type: `Scrum`
5. **Запишите Board ID** из URL (например: `1`) → вставьте в `Config.AGILE_BOARD_ID`

### Шаг 4: Получение API Token (2 минуты)

1. Аватар → **Profile** → **Authentication**
2. **Create Token**: `Import Script`
3. Permissions: `Read Project, Write Project, Read Issues, Write Issues, Read Agile, Write Agile`
4. Вставьте токен в `Config.TOKEN`

### Шаг 5: Обновление конфигурации (1 минута)

Обновите `Config` объект в `YouTrackImport.kt`:

```kotlin
object Config {
    val YOUTRACK_URL = "http://localhost:8081"
    val TOKEN = "ваш-токен"
    val EXISTING_PROJECT_ID = "0-8"
    val EXISTING_PROJECT_SHORTNAME = "NOUS"
    val AGILE_BOARD_ID = "1"  // Из Шага 3
    val COMPONENTS_FIELD_ID = "177-54"
    val SPRINT_FIELD_ID = "177-59"  // Найдите после создания поля Sprint
}
```

## Как найти Field IDs:
### Через API
curl -H "Authorization: Bearer YOUR_TOKEN" \
  "http://localhost:8081/api/admin/projects/0-8/customFields?fields=id,name"