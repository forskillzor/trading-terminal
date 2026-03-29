# Инструкция по настройке YouTrack для проекта Nous Platform

## 📋 Что уже сделано

✅ **Проект создан:** NOUS (ID: 0-8)  
✅ **Компоненты добавлены:** 32 компонента в поле "Subsystem"  
✅ **Эпики созданы:** 5 эпиков (NOUS-761 до NOUS-765)  
✅ **Задачи созданы:** ~50 задач (NOUS-766 до NOUS-819)  
✅ **Компоненты привязаны:** Большинство задач имеют правильные компоненты

## 🔧 Что нужно настроить вручную через UI YouTrack

### 1. Создание кастомных полей

В проекте NOUS создайте следующие поля через UI:

#### Поле "Type" (Тип задачи)
- **Тип:** Enum (одиночный выбор)
- **Значения:** Task, Feature, Bug, Epic, Spike, Improvement
- **По умолчанию:** Task

#### Поле "Priority" (Приоритет)
- **Тип:** Enum (одиночный выбор)
- **Значения:** Low, Medium, High, Critical
- **По умолчанию:** Medium

#### Поле "Story Points" (Оценка)
- **Тип:** Enum (одиночный выбор)
- **Значения:** 1, 2, 3, 5, 8, 13, 21, 34, 55
- **По умолчанию:** 3

#### Поле "Status" (Статус)
- **Тип:** State (Workflow)
- **Значения:** Backlog, To Do, In Progress, Code Review, Testing, Done
- **По умолчанию:** Backlog

### 2. Настройка Workflow

Создайте workflow "Development Workflow" со следующими переходами:

```
Backlog → To Do          (Ready for work)
To Do → In Progress      (Start work)
In Progress → Code Review (Submit for review)
Code Review → Testing    (Approve)
Code Review → In Progress (Request changes)
Testing → Done           (Pass testing)
Testing → In Progress    (Fail testing)
Backlog → In Progress    (Start immediately)
```

### 3. Создание Agile Board (Scrum)

Создайте Scrum доску "Nous Platform Board":

#### Колонки:
- **Backlog** (состояние: Backlog)
- **To Do** (состояние: To Do)
- **In Progress** (состояние: In Progress)
- **Code Review** (состояние: Code Review)
- **Testing** (состояние: Testing)
- **Done** (состояние: Done)

#### Swimlanes (по эпикам):
- Epic 1: Этап 1: Фундамент и MVP Read-Only
- Epic 2: Этап 2: IDE и плагинная система
- Epic 3: Этап 3: DSL и бэктестинг
- Epic 4: Этап 4: Live Trading и монетизация
- Epic 5: Этап 5: Масштабирование и экосистема

#### Card Layout (поля на карточке):
- Summary
- Assignee
- Priority
- Estimate
- Sprint

### 4. Создание спринтов

Создайте 11 спринтов согласно плану:

| Спринт | Название | Цель | Даты | Емкость |
|--------|----------|------|------|---------|
| Sprint 1 | KMP Foundation | Базовая архитектура KMP + Gradle + DI + тема | 2026-03-15 - 2026-03-28 | 40 |
| Sprint 2 | Binance Integration | Подключение к Binance WebSocket + REST API | 2026-03-29 - 2026-04-11 | 45 |
| Sprint 3 | DOM Widget | Стакан заявок с визуализацией и настройками | 2026-04-12 - 2026-04-25 | 50 |
| Sprint 4 | Chart & Trades | График свечей + лента сделок в реальном времени | 2026-04-26 - 2026-05-09 | 50 |
| Sprint 5 | Terminal Layout | Терминальный интерфейс с перетаскиваемыми панелями | 2026-05-10 - 2026-05-23 | 55 |
| Sprint 6 | Portfolio & Trading | Портфель + размещение ордеров + Paper Trading | 2026-05-24 - 2026-06-06 | 50 |
| Sprint 7 | Launch Prep | Подготовка к публичному запуску + waitlist | 2026-06-07 - 2026-06-20 | 45 |
| Sprint 8 | User Growth | Первые 100 пользователей + контент-маркетинг | 2026-06-21 - 2026-07-04 | 45 |
| Sprint 9 | Monetization | Pro подписка + первые платящие + Plugin SDK | 2026-07-05 - 2026-07-18 | 50 |
| Sprint 10 | Marketplace | Маркетплейс плагинов + Bybit Provider + Security | 2026-07-19 - 2026-08-01 | 50 |
| Sprint 11 | Scale | DSL + бэктестинг + масштабирование + релизный пайплайн | 2026-08-02 - 2026-08-15 | 40 |

### 5. Назначение задач на спринты

После создания спринтов, назначьте задачи на соответствующие спринты:

- **Sprint 1:** NOUS-1 до NOUS-5
- **Sprint 2:** NOUS-6 до NOUS-11
- **Sprint 3:** NOUS-12 до NOUS-17
- **Sprint 4:** NOUS-18 до NOUS-23
- **Sprint 5:** NOUS-24 до NOUS-27
- **Sprint 6:** NOUS-28 до NOUS-32
- **Sprint 7:** NOUS-33 до NOUS-37
- **Sprint 8:** NOUS-38 до NOUS-42
- **Sprint 9:** NOUS-43 до NOUS-46
- **Sprint 10:** NOUS-47 до NOUS-50
- **Sprint 11:** NOUS-51 до NOUS-55

## 🚀 Использование импорт-скриптов

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

### Конфигурация
Все настройки находятся в файлах:
- `youtrack-import/src/main/kotlin/project.json` - структура проекта
- `youtrack-import/src/main/kotlin/YouTrackImport.kt` - основной импортер
- `youtrack-import/src/main/kotlin/AddComponents.kt` - импортер компонентов

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