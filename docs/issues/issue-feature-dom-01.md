# Issue: Анализ проблем в коде DomWindow и компонентов

**ID:** `issue-feature-dom-01`  
**Дата:** 2026-04-18  
**Статус:** Открыт  
**Приоритет:** Средний  
**Область:** `features/feature-dom`  
**Затронутые файлы:** 
- `DomWindow.kt`
- `DomViewModel.kt` 
- `DomHeader.kt`
- `SplitDomContent.kt`
- `UnifiedDomContent.kt`

## Описание проблемы

Код компонентов DOM (Depth of Market) функционален, но имеет архитектурные проблемы, которые затрудняют поддержку, тестирование и дальнейшее развитие. Пользователь отмечает дискомфорт при работе с кодом, хотя не может точно сформулировать причину.

## Детальный анализ

### 1. Сложная логика управления подписками в DomViewModel

**Файл:** `DomViewModel.kt` (строки 109-157)

**Проблема:** Метод `restartSubscription()` содержит сложную логику ветвления для двух режимов (UNIFIED/SPLIT) с дублированием кода обработки ошибок.

**Пример проблемного кода:**
```kotlin
subscriptionJob = viewModelScope.launch {
    when (options.mode) {
        DomMode.UNIFIED -> {
            domRepository.subscribeToUnifiedOrderBook(...)
                .catch { e -> ... }  // Дублирование
                .collect { unifiedData -> ... }
        }
        
        DomMode.SPLIT -> {
            domRepository.subscribeToOrderBook(...)
                .catch { e -> ... }  // Дублирование
                .collect { data -> ... }
            
            domRepository.getBookTicker(...)
                .catch { e -> ... }  // Дублирование
                .collect { prices -> ... }
        }
    }
}
```

**Симптомы:**
- Трудно читать и поддерживать
- Дублирование логики обработки ошибок
- Нарушение принципа DRY (Don't Repeat Yourself)

### 2. Нарушение принципа единственной ответственности (SRP)

**Проблема:** Класс `DomViewModel` (233 строки) отвечает за слишком многое:
- Управление подписками на данные
- Обработку торговых команд
- Управление состоянием UI
- Загрузку информации о символах (tickSize)
- Обработку ошибок

**Симптомы:**
- Класс слишком большой и сложный
- Трудно тестировать изолированно
- Высокая связность (coupling)

### 3. Проблемы с агрегацией данных

**Проблема:** Несогласованность в реализации агрегации:
- В `DomWindow.kt` (строки 42-52) агрегация выполняется для UNIFIED режима
- В `SplitDomContent.kt` строка 17: "todo не работает агрегация. Ее здесь даже нет."

**Затронутые файлы:**
- `DomWindow.kt`: строки 42-52 (работает для UNIFIED)
- `SplitDomContent.kt`: строка 17 (TODO комментарий)

**Симптомы:**
- Несогласованное поведение между режимами
- Частичная реализация функциональности
- Технический долг

### 4. Сложная логика отображения в DomWindow

**Файл:** `DomWindow.kt` (строки 70-108)

**Проблема:** Много условной логики для определения `dataAvailable` и выбора режима отображения.

**Пример:**
```kotlin
val dataAvailable = when (domOptions.mode) {
    DomMode.SPLIT -> orderBook != null
    DomMode.UNIFIED -> displayUnifiedOrderBook != null
}

if (!dataAvailable) {
    // Показать индикатор загрузки
} else {
    when (domOptions.mode) {
        DomMode.SPLIT -> { ... }
        DomMode.UNIFIED -> { ... }
    }
}
```

**Симптомы:**
- Вложенные условия усложняют чтение кода
- Смешение логики отображения и бизнес-логики

### 5. Проблемы с управлением состоянием

**Проблема:** Использование `!!` (not-null assertion) после проверки на null.

**Примеры:**
- `DomWindow.kt` строка 85: `orderBook!!` после проверки `orderBook != null`
- `DomWindow.kt` строка 97: `displayUnifiedOrderBook!!` после проверки

**Симптомы:**
- Риск `NullPointerException` при рефакторинге
- Нарушение безопасных практик Kotlin

### 6. Неэффективное использование корутин

**Файл:** `DomViewModel.kt` (строки 33-34)

**Проблема:** Создание собственного `dispatcher` и `viewModelScope`:
```kotlin
private val dispatcher = Executors.newSingleThreadExecutor().asCoroutineDispatcher()
private val viewModelScope = CoroutineScope(dispatcher + SupervisorJob())
```

**Симптомы:**
- Ручное управление жизненным циклом корутин
- Риск утечек памяти
- Неиспользование стандартного `viewModelScope` из Android

### 7. Проблемы в компонентах UI

#### DomHeader
**Проблема:** Сложная вложенная структура с дублированием layout кода.
**Файл:** `DomHeader.kt` (строки 50-211)

#### SplitDomContent
**Проблема:** Сложная логика автоскролла смешана с бизнес-логикой.
**Файл:** `SplitDomContent.kt` (строки 42-72)

#### TODO комментарии
**Проблема:** Много незавершенных задач:
- `DomHeader.kt` строки 132-138: замена SymbolDropdown на поле ввода
- `SplitDomContent.kt` строка 17: отсутствие агрегации
- `DomViewModel.kt` строка 124: TODO про агрегацию

## Рекомендации по улучшению

### 1. Рефакторинг DomViewModel

**Предлагаемая структура:**
```kotlin
// Разделить на отдельные классы:
class SubscriptionManager(
    private val domRepository: DomRepository,
    private val errorHandler: ErrorHandler
)

class TradingCommandExecutor(
    private val domRepository: DomRepository,
    private val commandFactory: TradingCommandFactory
)

class SymbolInfoLoader(
    private val symbolInfoRepository: SymbolInfoRepository?
)

class DomAggregationService(
    private val tickSizeProvider: TickSizeProvider
)
```

### 2. Упростить логику подписок

**Использовать паттерн Strategy:**
```kotlin
interface SubscriptionStrategy {
    fun subscribe(options: DomOptions): Flow<DomData>
}

class UnifiedSubscriptionStrategy : SubscriptionStrategy { ... }
class SplitSubscriptionStrategy : SubscriptionStrategy { ... }
```

### 3. Улучшить управление состоянием

**Заменить `!!` на безопасные конструкции:**
```kotlin
// Вместо:
orderBook!!

// Использовать:
orderBook?.let { safeOrderBook ->
    // Работа с safeOrderBook
}

// Или:
requireNotNull(orderBook) { "OrderBook должен быть не null" }
```

### 4. Оптимизировать компоненты UI

**Разбить DomHeader на меньшие компоненты:**
```kotlin
@Composable
fun ProviderSelector(...)
@Composable  
fun SymbolSelector(...)
@Composable
fun AggregationSelector(...)
```

**Вынести логику автоскролла:**
```kotlin
@Composable
fun useAutoScroll(
    listState: LazyListState,
    scrollToTop: Boolean,
    idleTimeout: Long = 5000
): Boolean
```

### 5. Улучшить архитектуру

**Применить Dependency Injection:**
- Использовать Koin для инъекции зависимостей
- Создать отдельные модули для разных ответственностей

**Использовать StateFlow с stateIn:**
```kotlin
val orderBook: StateFlow<OrderBook?> = domRepository
    .subscribeToOrderBook(...)
    .catch { emit(null) }
    .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )
```

### 6. Решить технический долг

**Приоритетные задачи:**
1. Реализовать агрегацию для SPLIT режима
2. Заменить SymbolDropdown на поле ввода с автодополнением
3. Добавить сохранение избранных символов
4. Убрать все `!!` операторы
5. Реализовать единую обработку ошибок

## Оценка усилий

| Задача | Сложность | Время (часы) |
|--------|-----------|--------------|
| Рефакторинг DomViewModel | Высокая | 8-12 |
| Реализация агрегации для SPLIT | Средняя | 4-6 |
| Замена SymbolDropdown | Средняя | 6-8 |
| Улучшение управления состоянием | Низкая | 2-4 |
| Оптимизация компонентов UI | Средняя | 4-6 |
| **Итого** | | **24-36** |

## Критерии успеха

1. Уменьшение размера `DomViewModel` на 50%
2. Устранение всех `!!` операторов
3. Реализация агрегации для обоих режимов
4. Улучшение покрытия тестами на 30%
5. Устранение дублирования кода в обработке ошибок

## Следующие шаги

1. Создать ветку для рефакторинга: `refactor/dom-architecture`
2. Начать с рефакторинга `DomViewModel`
3. Реализовать агрегацию для SPLIT режима
4. Постепенно заменять компоненты
5. Добавить unit-тесты для новых классов

---

*Сгенерировано автоматически на основе анализа кода. Для обсуждения деталей реализации создайте отдельную задачу.*