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

| Field Name     | Type     | Values                                                    |
|----------------|----------|-----------------------------------------------------------|
| `Story Points` | Enum     | `1, 2, 3, 5, 8, 13, 21, 34, 55`                           |
| `Priority`     | Enum     | `Low, Medium, High, Critical`                             |
| `Type`         | Enum     | `Task, Feature, Bug, Epic, Spike, Improvement`            |
| `Status`       | State    | `Backlog, To Do, In Progress, Code Review, Testing, Done` |
| `Sprint`       | State    | *(создаётся автоматически при создании спринтов)*         |
| `Components`   | EnumMult | Core-Base, Core-Domain, Core-Network, etc.                |


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


### ID проекта в youtrack выполните в терминале
```Bash
curl -H "Authorization: Bearer perm-YWRtaW4=.NDItMQ==.LLbeWWAZFVOXicroCXshF8D2sPvAm8" \
     -H "Accept: application/json" \
     http://localhost:8081/api/admin/projects?fields=id,name,shortName
```

### Пример ответа 
```json
[
  {
    "id": "0-2",
    "name": "NOUS Platform", 
    "shortName": "NOUS"
  }
]
```
Обновите `Config` объект в `YouTrackImport.kt`:
```kotlin
object Config {
    val YOUTRACK_URL = "http://localhost:8081"
    val TOKEN = "ваш-токен"
    val EXISTING_PROJECT_ID = "0-8"
    val EXISTING_PROJECT_SHORTNAME = "NOUS"
    val AGILE_BOARD_ID = "1"  // Из Шага 3
    
    // Delays между запросами (в миллисекундах)
    val EPIC_DELAY_MS = 500L
    val TASK_DELAY_MS = 300L
    val SPRINT_DELAY_MS = 400L
}
```

## 🚀 НОВАЯ ФУНКЦИОНАЛЬНОСТЬ: Автоматическое определение полей

**Скрипт теперь автоматически определяет все Field IDs через API!** Больше не нужно вручную искать и вставлять ID полей.

### Как это работает:

1. **Автоматическое обнаружение полей**: Скрипт запрашивает список всех custom fields проекта через API
2. **Интеллектуальное сопоставление**: Находит поля по имени (Story Points, Priority, Type, Status, Sprint)
3. **Автоматическое определение Components**: Ищет поле "Subsystem" или "Components" для привязки компонентов
4. **Частичное совпадение**: Если точное имя не найдено, ищет поля с похожими названиями

### Преимущества:
- ✅ Не нужно вручную искать Field IDs
- ✅ Адаптация к разным названиям полей
- ✅ Автоматическое обновление при изменении структуры проекта
- ✅ Более надежная работа скрипта

## 🏃 Запуск импорта

### Скрипт 1: Импорт компонентов
```bash
cd youtrack-import
# Измените mainClass в build.gradle.kts на "AddComponentsKt"
../gradlew run
```

### Скрипт 2: Импорт задач и эпиков
```bash
cd youtrack-import
# Измените mainClass в build.gradle.kts на "YouTrackImportKt"
../gradlew run
```

## 🔍 Проверка результатов

После настройки проверьте:

1. **Проект:** http://localhost:8081/issues?q=project:NOUS
2. **Компоненты:** Убедитесь, что поле "Subsystem" содержит 32 значения
3. **Эпики:** 5 эпиков с правильными названиями
4. **Задачи:** ~50 задач с компонентами и описаниями
5. **Workflow:** Статусы работают корректно
6. **Agile Board:** Доска отображает задачи по колонкам

## 🛠️ Решение проблем

### Проблема: Ошибки при создании задач
**Решение:** Проверьте, что все компоненты существуют в поле "Subsystem". Если нет, запустите `AddComponents.kt` повторно.

### Проблема: Поля Type/Priority не работают
**Решение:** Создайте поля вручную через UI YouTrack перед запуском импорта.

### Проблема: Символы "/" в названиях компонентов
**Решение:** YouTrack не принимает символ "/" в значениях enum полей. Используйте дефис "-" вместо "/".

### Проблема: Скрипт не находит поля
**Решение:** Убедитесь, что:
1. YouTrack доступен по указанному URL
2. Токен имеет необходимые permissions
3. Поля созданы с правильными именами (см. Шаг 2)

## 📊 Статистика проекта

- **Всего эпиков:** 5
- **Всего задач:** 55
- **Компонентов:** 32
- **Спринтов:** 11
- **Продолжительность:** ~5 месяцев (март - август 2026)
- **Общая емкость:** ~520 story points

## 🎯 Рекомендации по использованию

1. **Ежедневные стендапы:** Используйте Agile Board для визуализации прогресса
2. **Ретроспективы:** После каждого спринта анализируйте метрики в YouTrack
3. **Приоритизация:** Используйте поле Priority для управления очередностью
4. **Оценка:** Используйте Story Points для планирования емкости спринтов
5. **Компоненты:** Назначайте задачи на соответствующие компоненты для лучшего отслеживания

## 🔗 Полезные ссылки

- **YouTrack документация:** https://www.jetbrains.com/help/youtrack/
- **REST API:** http://localhost:8081/api
- **Проект NOUS:** http://localhost:8081/issues?q=project:NOUS
- **Agile Board:** После создания будет доступна в разделе Agile

---

**Готово к работе!** 🚀

Проект Nous Platform полностью настроен в YouTrack с эпиками, задачами, компонентами и структурой спринтов. Осталось только создать кастомные поля и Agile Board через UI, после чего можно начинать работу по методологии Scrum.