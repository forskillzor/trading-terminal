# Полное руководство по работе с YouTrack для проекта Nous Platform

## 📋 Оглавление
1. [Настройка проекта в YouTrack](#настройка-проекта-в-youtrack)
2. [Создание компонентов](#создание-компонентов)
3. [Создание Epic и задач](#создание-epic-и-задач)
4. [Настройка workflow](#настройка-workflow)
5. [Интеграция с Git](#интеграция-с-git)
6. [Создание спринтов](#создание-спринтов)
7. [Ежедневная работа](#ежедневная-работа)
8. [Метрики и отчеты](#метрики-и-отчеты)

## Настройка проекта в YouTrack

### Шаг 1: Открыть YouTrack плагин в IntelliJ
1. **View → Tool Windows → YouTrack** (или Alt+Y)
2. Убедись, что подключен к серверу YouTrack
3. Если нет подключения: **Settings → Tools → YouTrack → Add Server**

### Шаг 2: Создать проект "NOUS"
1. В YouTrack плагине нажми **Create New Project**
2. Заполни данные:
   ```
   Project ID: NOUS
   Name: Nous Platform
   Description: Kotlin Multiplatform trading platform with plugin system
   Template: Scrum (рекомендуется)
   Lead: [твой логин]
   ```
3. Нажми **Create**

### Шаг 3: Настройка проекта
1. Перейди в **Project Settings**
2. Настрой:
   - **Time tracking:** Включить
   - **Voting:** По желанию
   - **Comments:** Разрешить всем
   - **Attachments:** Разрешить

## Создание компонентов

### Список компонентов для создания:
Создай следующие компоненты в **Project Settings → Components → Create Component**:

| ID | Name | Description |
|----|------|-------------|
| Core-Base | Core-Base | utils, extensions, math |
| Core-Domain | Core-Domain | entities, repositories, usecases |
| Core-Network | Core-Network | http, websocket, client |
| Core-DI | Core-DI | CoreModule, DI |
| Core-Theme | Core-Theme | Colors, Typography, Theme |
| Feature-Chart | Feature-Chart | графики, свечи |
| Feature-DOM | Feature-DOM | стакан заявок |
| Feature-Trades | Feature-Trades | лента сделок |
| Feature-Terminal | Feature-Terminal | лейаут, панели |
| Feature-Portfolio | Feature-Portfolio | портфель, позиции |
| Feature-Indicators | Feature-Indicators | индикаторы |
| Feature-Strategies | Feature-Strategies | стратегии |
| Plugin-Loader | Plugin-Loader | сканирование JAR |
| ClassLoader | ClassLoader | изолированные ClassLoader'ы |
| Security | Security | подписи, разрешения |
| Registry | Registry | PluginRegistry, PluginManifest |
| Market-API | Market-API | MarketDataProvider |
| Trading-API | Trading-API | TradingProvider |
| Account-API | Account-API | AccountProvider |
| Indicators-API | Indicators-API | Indicator |
| Strategies-API | Strategies-API | Strategy |
| Robots-API | Robots-API | TradingRobot |
| UI-API | UI-API | Widget, ChartWidget |
| Binance-Provider | Binance-Provider | Binance провайдер |
| Bybit-Provider | Bybit-Provider | Bybit провайдер |
| Build-System | Build-System | Gradle, KMP |
| CI-CD | CI/CD | GitHub Actions |
| Database | Database | SQLite |
| Monitoring-Logging | Monitoring/Logging | логирование |

## Создание Epic и задач

### Шаг 1: Создать Epic
1. **Create New Issue** в плагине
2. Заполни:
   ```
   Project: NOUS
   Type: Epic
   Summary: MVP с Live Trading (v0.1.0)
   Description: Запуск минимально жизнеспособного продукта с поддержкой реальной торговли на Binance
   Priority: High
   Component: Epic
   ```
3. Сохрани как `NOUS-EPIC-1`

### Шаг 2: Импорт задач из JSON
Используй файл `youtrack-tasks.json` для создания задач:

1. **Метод A: Ручное создание** (для каждой задачи):
   - **Create New Issue**
   - Используй данные из JSON
   - Пример для NOUS-1:
     ```
     Project: NOUS
     Type: Task
     Summary: Настроить Kotlin Multiplatform проект с Gradle
     Description: [из JSON]
     Component: Build-System
     Priority: High
     Estimation: 5
     Epic: MVP с Live Trading (v0.1.0)
     ```

2. **Метод B: Импорт через API** (скрипт):
   - Установи переменную окружения: `export YOUTRACK_TOKEN=твой_токен`
   - Запусти: `kotlin scripts/create-youtrack-tasks.kt`

## Настройка workflow

### Основной workflow для задач:
```
Backlog → To Do → In Progress → Code Review → Testing → Done
```

### Настройка статусов:
1. **Project Settings → Workflows**
2. Создай workflow "Development Workflow":
   ```
   States:
   - Backlog (начальный)
   - To Do
   - In Progress
   - Code Review
   - Testing
   - Done (финальный)
   
   Transitions:
   - Backlog → To Do: "Ready for work"
   - To Do → In Progress: "Start work"
   - In Progress → Code Review: "Submit for review"
   - Code Review → Testing: "Approve"
   - Code Review → In Progress: "Request changes"
   - Testing → Done: "Pass testing"
   - Testing → In Progress: "Fail testing"
   ```

### Workflow для багов:
```
Open → In Progress → Fixed → Verified → Closed
```

## Интеграция с Git

### Шаг 1: Настройка в YouTrack
1. **Project Settings → Integrations → VCS**
2. Подключи GitHub репозиторий
3. Настрой:
   - **Commit message format:** `[NOUS-{id}] {summary}`
   - **Branch name format:** `{type}/{id}-{summary}`
   - **Auto-link commits:** Включить
   - **Auto-transition:** Включить (при коммите → In Progress, при мерже → Testing)

### Шаг 2: Настройка в Git
Создай файл `.github/PULL_REQUEST_TEMPLATE.md`:
```markdown
## Описание изменений

## Связанные задачи
- Closes NOUS-123

## Тип изменений
- [ ] Новая функциональность
- [ ] Исправление бага
- [ ] Рефакторинг

## Чеклист
- [ ] Код соответствует стилю проекта
- [ ] Добавлены тесты
- [ ] Документация обновлена
```

### Шаг 3: Конвенции Git
- **Коммиты:** `[NOUS-123] Краткое описание`
- **Ветки:**
  - Фичи: `feature/NOUS-123-short-desc`
  - Баги: `bugfix/NOUS-456-fix-issue`
  - Хотфиксы: `hotfix/NOUS-789-critical`
  - Релизы: `release/v1.0.0`

## Создание спринтов

### Шаг 1: Планирование спринта
1. Открой **Agile → Create Sprint**
2. Заполни:
   ```
   Name: Sprint 1 - MVP Foundation
   Goal: Создать базовую архитектуру и подключение к Binance
   Dates: [дата начала] - [дата окончания] (2 недели)
   ```
3. Добавь задачи из файлов спринтов:
   - `sprints/sprint-1-mvp-foundation.md`
   - `sprints/sprint-2-ui-components.md`
   - `sprints/sprint-3-trading-features.md`

### Шаг 2: Распределение задач
1. Перетащи задачи из бэклога в спринт
2. Установи оценки (story points)
3. Назначь ответственных
4. Установи зависимости между задачами

### Шаг 3: Настройка доски
1. **Agile → Boards → Create Board**
2. Выбери тип: **Scrum Board**
3. Настрой колонки:
   ```
   To Do
   In Progress
   Code Review
   Testing
   Done
   ```
4. Добавь swimlanes по компонентам или приоритетам

## Ежедневная работа

### Ежедневный стендап
Используй шаблон из `templates/daily-standup.md`:
```
### [Дата] Daily Standup

**Что сделал вчера:**
- 

**Что планирую сделать сегодня:**
- 

**Блокеры:**
- 
```

### Работа с задачами
1. **Взять задачу в работу:**
   - Найди задачу в To Do
   - Нажми "Start work" → статус меняется на In Progress
   - Создай ветку: `feature/NOUS-123-short-desc`

2. **Работа над задачей:**
   - Коммить с префиксом: `[NOUS-123]`
   - Когда готово: создай PR
   - Нажми "Submit for review" → статус Code Review

3. **Code Review:**
   - Проверь код
   - Если ок: "Approve" → статус Testing
   - Если нужны правки: "Request changes" → статус In Progress

4. **Тестирование:**
   - Протестируй задачу
   - Если ок: "Pass testing" → статус Done
   - Если баги: "Fail testing" → статус In Progress

### Трекинг времени
1. В задаче нажми **Start Timer** когда начинаешь работу
2. **Stop Timer** когда заканчиваешь
3. YouTrack автоматически считает затраченное время

## Метрики и отчеты

### Ключевые метрики
1. **Velocity:** Story points за спринт
2. **Burndown:** Оставшиеся story points по дням
3. **Cycle Time:** Время от начала до завершения задачи
4. **Lead Time:** Время от создания до завершения задачи

### Отчеты
1. **Burndown Chart:** Agile → Reports → Burndown
2. **Velocity Chart:** Agile → Reports → Velocity
3. **Workload:** Agile → Reports → Workload
4. **Time Tracking:** Reports → Time Tracking

### Ретроспектива
После каждого спринта:
1. Собери команду
2. Обсуди по шаблону из `templates/retrospective.md`
3. Зафиксируй action items
4. Внеси улучшения в следующий спринт

## Полезные горячие клавиши
- **Alt+Y:** Открыть YouTrack панель
- **Ctrl+Alt+N:** Создать новую задачу
- **Ctrl+Alt+F:** Поиск задач
- **Ctrl+Alt+U:** Обновить список задач

## Troubleshooting

### Проблема: Не видно проекта
**Решение:** Проверь подключение к серверу в Settings → Tools → YouTrack

### Проблема: Не создаются задачи
**Решение:** Убедись, что у тебя есть права на создание задач в проекте

### Проблема: Не работает интеграция с Git
**Решение:**
1. Проверь настройки VCS в YouTrack
2. Убедись, что коммиты содержат ID задач
3. Проверь webhook в GitHub/GitLab

### Проблема: Не обновляются статусы
**Решение:** Проверь workflow transitions и права

## Дополнительные ресурсы
- [YouTrack Documentation](https://www.jetbrains.com/help/youtrack/)
- [IntelliJ YouTrack Plugin Guide](https://www.jetbrains.com/help/idea/youtrack-integration.html)
- [Git Integration Guide](https://www.jetbrains.com/help/youtrack/incloud/git-hub-integration.html)

---

**Следующие шаги:**
1. Создай проект NOUS в YouTrack
2. Импортируй компоненты и задачи
3. Настрой интеграцию с Git
4. Создай первый спринт
5. Начни работу по workflow

Удачи в разработке Nous Platform! 🚀