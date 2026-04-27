# Оптимизация производительности DOM

## Анализ текущей реализации (корректная ветка)

Текущая архитектура корректна, но имеет 6 узких мест по производительности.

### Data Flow (as-is)

```
DepthUpdate → DomRepositoryImpl → DomEvent → DomViewModel → StateFlow → DomWindow → OrderBook → DomSection → LazyColumn
                                       ↑              ↑              ↑              ↑
                                 List<DomEvent>  toMutableMap()  remember()   groupBy+sum+map
                                  (аллокация)    (O(N) копия)    (полный      (полный
                                                                пересбор)    пересбор)
```

### Проблема 1: Полная копия Map в ViewModel на каждый UpdateBid/UpdateAsk (🔴 КРИТИЧЕСКАЯ)

```kotlin
// DomViewModel.kt:178-185
is DomEvent.UpdateBid -> {
    val currentBids = _incrementalBids.value.toMutableMap() // ← O(N) аллокация всей карты!
    currentBids[event.price] = event.quantity               // O(1) изменение ONE entry
    _incrementalBids.value = currentBids                    // StateFlow emit → recomposition
}
```

**Почему плохо:** При ~100-1000 depth-обновлений/сек каждое меняет 1 цену, но копируется вся карта из ~500+ entry.

**Последствия:**
- Каждый `toMutableMap()` создаёт новый `HashMap` размером с оригинал → O(N) аллокаций/сек
- Каждый `StateFlow.value = newMap` триггерит `collectAsState()` → перезапуск `remember` в DomWindow → полный пересбор уровней
- GC pressure от постоянного создания и уничтожения HashMap

### Проблема 2: Полный пересбор unified уровней в DomWindow на каждый чих (🔴 КРИТИЧЕСКАЯ)

```kotlin
// DomWindow.kt:44-111
val displayUnifiedOrderBook =
    remember(aggregation, tickSize, incrementalBids, incrementalAsks, bestBid, bestAsk) {
        // 1. Итерация ВСЕХ bids + asks + создание OrderBookLevel
        // 2. sortByDescending
        // 3. aggregate() → groupBy + sumOf + map
    }
```

**Почему плохо:** `incrementalBids` и `incrementalAsks` — это StateFlow, которые меняются на КАЖДЫЙ UpdateBid/UpdateAsk. Одно изменение цены → пересбор всего списка уровней.

**Измерение:** При 500 bids + 500 asks:
- Проход обеих карт: 1000 итераций
- Создание OrderBookLevel: 1000 аллокаций
- Сортировка: 1000*log(1000) ≈ 10000 сравнений
- Агрегация: ещё 1000 итераций + groupBy + sumOf

### Проблема 3: String↔Double конверсия туда-сюда (🟡 СРЕДНЯЯ)

```
OrderBookState: ConcurrentHashMap<String, String>
    → DomEvent: toDoubleOrNull() → Double
    → ViewModel: Map<Double, Double>
    → DomWindow: price.toString(), qty.toString() → OrderBookLevel(price: String, quantity: String)
    → DomSection: level.price.toDoubleOrNull() → Double (сортировка, скролл)
    → DomAggregator: level.price.toDoubleOrNull(), qty.toDoubleOrNull(), aggregationKey(price: String) → String
```

Каждая конверсия — аллокация строки (или её парсинг). При 500+ уровнях × 100+ обновлений/сек = 50000+ лишних конверсий.

### Проблема 4: Промежуточный List<DomEvent> в fromDepthUpdate (🟡 СРЕДНЯЯ)

```kotlin
// DomRepositoryImpl.kt:92-94
DomEvent.fromDepthUpdate(depthUpdate, symbol) // ← создаёт List<DomEvent>
    .forEach { event -> trySend(event) }       // ← сразу consumed
```

Каждый depthUpdate содержит 50-100 bid/ask изменений → аллокация MutableList + N объектов DomEvent.

### Проблема 5: groupBy в DomAggregator агрегация (🟢 ЛЁГКАЯ)

```kotlin
// DomAggregator.kt:107
val grouped = unifiedLevels.groupBy { ... } // создаёт Map<List>
    .map { (key, group) -> ... group.sumOf { ... } ... } // ещё одна аллокация списка
```

`groupBy` аллоцирует промежуточную `Map<String, List<OrderBookLevel>>` с вложенными списками. Можно заменить на один проход с `LinkedHashMap`.

### Проблема 6: Redundant aggregationKey в DomSection скролле (🟢 ЛЁГКАЯ)

При каждой проверке видимости target-цены вызывается `aggregationLevel.aggregationKey(price.toString(), tickSize)` в цикле по visibleItems. Каждый вызов конвертирует Double→String, ищет по карте, конвертирует обратно.

## План оптимизаций

### Шаг 1: ViewModel — SnapshotStateMap вместо StateFlow<Map> (🔴 КРИТ)

**Что меняем:**
- `MutableStateFlow<Map<Double, Double>>` → `SnapshotStateMap<Double, Double>` (как в нашей предыдущей упрощённой версии)
- `_incrementalBids.value = emptyMap()` → `incrementalBids.clear()`
- `val currentBids = _incrementalBids.value.toMutableMap()` → `incrementalBids[price] = quantity`
- `_incrementalBids.value = currentBids` → удалить (in-place мутация)

**Эффект:**
- Устраняем O(N) копию на каждое обновление
- Compose отслеживает ТОЛЬКО изменённые entry для рекомпозиции
- Без изменения сигнатуры для UI (`Map<Double, Double>` — SnapshotStateMap implements Map)

**Важно:** Убираем отдельные `_incrementalBestBid/Ask` StateFlow — best bid/ask вычисляются через `derivedStateOf` из карт (как мы уже сделали ранее).

### Шаг 2: DomWindow — derivedStateOf вместо remember (🔴 КРИТ)

**Что меняем:**
- Вместо `remember(..., incrementalBids, incrementalAsks, ...)` используем `derivedStateOf` который читает из `SnapshotStateMap`
- Compose будет пересчитывать `derivedStateOf` только когда изменятся прочитанные entry

Но здесь есть нюанс: `derivedStateOf` пересчитывается при изменении ЛЮБОГО entry, который был прочитан внутри него. Если мы итерируем ВСЕ bids и asks — то любое изменение любой цены всё равно триггерит пересчёт.

**Оптимальное решение:** Кэшировать отсортированный список уровней в ViewModel, обновляя его инкрементально:
- При UpdateBid/UpdateAsk: обновить entry в SnapshotStateMap и ОДНОВРЕМЕННО обновить кэш уровней (добавить/удалить/обновить один элемент, без пересортировки всех)
- Для этого нужна структура данных, поддерживающая sorted order + O(log n) обновление

**Упрощённое решение (без кэша):** Принимаем, что `derivedStateOf` пересчитывается часто, но минимизируем работу внутри него:
- Убираем создание `OrderBookLevel` с String-полями
- Оставляем сортировку (descents — быстрая, т.к. O(N log N) на ~1000 элементах)
- Агрегацию применяем уже к отсортированному списку

**Продвинутое решение (с кэшем):** Держим `SortedLevelMap<List<OrderBookLevel>>` в ViewModel, обновляем инкрементально.

### Шаг 3: Убрать String↔Double конверсию (🟡 СРЕДНЯЯ)

**Вариант A (рекомендуемый):** Изменить `OrderBookLevel` на использование Double:
```kotlin
data class OrderBookLevel(
    val price: Double,
    val quantity: Double = 0.0,
    val total: Double = 0.0,
    val bidQty: Double = 0.0,
    val askQty: Double = 0.0
)
```

**Проблема:** OrderBookLevel — часть public API (`api-market`), менять его нужно осторожно.

**Вариант B (внутренний):** Создать внутренний `DomLevel` data class в `feature-dom`:
```kotlin
internal data class DomLevel(
    val price: Double,
    val bidQty: Double,
    val askQty: Double
)
```

Использовать его для расчётов внутри feature-dom, конвертировать в `OrderBookLevel` только на границе с UI/API.

**Вариант C (минимальный):** Оставить `OrderBookLevel` как есть, но минимизировать конверсии в горячем пути. В DomWindow строить уровни напрямую с `price.toString()` — одну конверсию, а не три.

### Шаг 4: emitDepthUpdates callback вместо List (🟡 СРЕДНЯЯ)

Уже есть в нашем коде — `DomEvent.emitDepthUpdates(depthUpdate, symbol) { trySend(it) }`.
Просто используем его вместо `fromDepthUpdate`.

### Шаг 5: DomAggregator — single-pass вместо groupBy (🟢 ЛЁГКАЯ)

```kotlin
// Вместо:
val grouped = unifiedLevels.groupBy { aggregationKey }
return grouped.map { (key, group) -> ... }

// Используем:
val aggregated = linkedMapOf<String, MutableList<OrderBookLevel>>()
for (level in unifiedLevels) {
    val key = aggregationKey(level.price, baseTickSize)
    aggregated.getOrPut(key) { mutableListOf() }.add(level)
}
return aggregated.map { (key, group) -> ... }
```

Или ещё лучше — single-pass с накоплением сумм:
```kotlin
val result = linkedMapOf<String, AggregatedLevel>()
for (level in unifiedLevels) {
    val key = aggregationKey(level.price, baseTickSize)
    val existing = result.getOrPut(key) { AggregatedLevel(key) }
    existing.bidQty += level.bidQty.toDoubleOrNull() ?: 0.0
    existing.askQty += level.askQty.toDoubleOrNull() ?: 0.0
}
```

### Шаг 6: DomSection — precompute aggregation keys (🟢 ЛЁГКАЯ)

Вместо вызова `aggregationKey()` при каждой проверке скролла:
```kotlin
// Предвычислить ключи для всех уровней
val levelKeys = remember(levels, aggregationLevel, baseTickSize) {
    levels.map { aggregationLevel.aggregationKey(it.price, baseTickSize) }
}
// Использовать levelKeys[index] для сравнения
```

## Сводный план

| # | Оптимизация | Сложность | Эффект | Файлы |
|---|------------|-----------|--------|-------|
| 1 | **SnapshotStateMap** вместо StateFlow<Map> | Средняя | 🔴 Устраняет O(N) копию на каждый тик | ViewModel |
| 2 | **derivedStateOf + SnapshotStateMap** | Средняя | 🔴 Устраняет полный пересбор уровней | DomWindow |
| 3 | **emitDepthUpdates callback** | Низкая | 🟡 Устраняет промежуточный List | DomRepositoryImpl |
| 4 | **Внутренний DomLevel с Double** | Средняя | 🟡 Устраняет String↔Double конверсии | feature-dom |
| 5 | **Single-pass агрегация** | Низкая | 🟢 Устраняет groupBy оверхед | DomAggregator |
| 6 | **Предвычисление aggregation key** | Низкая | 🟢 Устраняет повторные конверсии | DomSection |

## Приоритет

1. **Шаг 1 + 2** (основные, ~80% прироста)
2. **Шаг 3 + 4** (важные, ~15% прироста)
3. **Шаг 5 + 6** (дополнительные, ~5% прироста)
