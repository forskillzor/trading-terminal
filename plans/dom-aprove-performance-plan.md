# DOM Performance: переход на scaled-Long модель и инкрементальный рендер

> **Статус**: Plan (согласован)
> **Ветка**: `dom-aprove-performance` (worktree, от `workspaces-refactoring` @ 61d910c)
> **Дата**: 2026-06-29
> **Цель**: убрать лаги при 12× DOM-гриде, не теряя точность цен/объёмов

---

## Оглавление

1. [Контекст и согласованные решения](#1-контекст-и-согласованные-решения)
2. [Что уже сделано — не трогаем](#2-что-уже-сделано--не-трогаем)
3. [Корень проблемы](#3-корень-проблемы)
4. [Концепция матчинг-движка: fixed-point (целочисленный масштаб)](#4-концепция-матчинг-движка-fixed-point-целочисленный-масштаб)
5. [Как применяем к нашему DOM](#5-как-применяем-к-нашему-dom)
6. [Целевой поток данных](#6-целевой-поток-данных)
7. [Модель данных: DomLevel](#7-модель-данных-domlevel)
8. [ViewModel: инкрементальная агрегация дельтой](#8-viewmodel-инкрементальная-агрегация-дельтой)
9. [depth = B: правило усечения](#9-depth--b-правило-усечения)
10. [UI: DomWindow / DomSection / LevelRow](#10-ui-domwindow--domsection--levelrow)
11. [Правильная рекомпозиция](#11-правильная-рекомпозиция)
12. [Граничные случаи](#12-граничные-случаи)
13. [Пошаговый план реализации](#13-пошаговый-план-реализации)
14. [Тесты](#14-тесты)
15. [Риски и митигации](#15-риски-и-митигации)
16. [Что НЕ трогаем / отложено](#16-что-не-трогаем--отложено)

---

## 1. Контекст и согласованные решения

12-грид (`Templates.domGrid`) создаёт N независимых DOM-панелей по разным символам. Каждая панель работает корректно по одной, но при 12 штуках всё лагает. Источник данных и сеть — **не** узкое место. Лаг — из-за обработки/отрисовки слишком большого числа уровней на каждый тик.

**Согласованные решения:**

| Решение | Выбор |
|---|---|
| Источник данных | Остаётся diff stream Binance + локальная книга + клиентская агрегация |
| Смысл `depth` при агрегации | **B** — `depth` = число строк отображения (агрегированных бакетов), глубокий вид |
| Внутренняя модель чисел | **scaled-Long** — цена и объём как целые `priceTicks: Long`, `qtySteps: Long` |
| `OrderBookLevel` (public-api, String) | НЕ трогаем |

---

## 2. Что уже сделано — не трогаем

Проверено в коммите `61d910c`:

- **Lifecycle/утечка закрыта**: `DomViewModel : Disposable`, `dispose()` → `viewModelScope.cancel()` (`DomViewModel.kt:73`). VM кэшируются per-workspace через `liveViewModels.getOrPut(vmKey)` и диспозятся при удалении панели (`main.kt:149,226`).
- **Проброс depth/aggregation** из `PanelState.Dom` в `DomOptions` есть (`main.kt:227-244`).
- **Путь ордера уже на Double**: `OrderIntent.price/quantity: Double`, `OrderData(price/quantity: Double)`. Точность ордера от нашего рефакторинга дисплея не зависит.

---

## 3. Корень проблемы

Текущий путь от карты до Compose гоняется на КАЖДЫЙ тик по ВСЕМ уровням, ×12 панелей:

```
SnapshotStateMap<Double,Double> (bids/asks, может быть 1000+ уровней)
 └─ derivedStateOf { buildDisplayOrderBook() }      ← дёргается на любой changed entry
     ├─ mutableMapOf<String, OrderBookLevel>()       ← новый HashMap каждый тик
     ├─ OrderBookLevel(price.toString(), qty.toString(), …)  ← N×String-аллокаций
     ├─ existing.copy(askQty=…)                       ← аллокации на мёрдж bid/ask
     ├─ sortedByDescending { price.toDoubleOrNull() } ← повторный парсинг всех N
     └─ aggregate() → aggregationKey(String) → buckets → toString()  ← ещё проход + строки
 └─ new OrderBook(levels = new List)                  ← новый список каждый тик
 └─ LazyColumn items(levels, key="level-$price")      ← String-ключ на айтем
     └─ LevelRow: toDoubleOrNull() + aggregationKey ×3 ← повторный парсинг на ряд
```

Три источника боли:
1. **O(N) ребилд всего** на каждый тик (а меняется обычно одна-две цены).
2. **Аллокации**: новый `List`, N×`OrderBookLevel`, тонны временных `String`.
3. **Конвертации типов** String↔Double туда-обратно, многократно.

---

## 4. Концепция матчинг-движка: fixed-point (целочисленный масштаб)

### 4.1. Зачем биржи не используют float

Настоящий **матчинг-движок** (ядро биржи, которое сводит заявки) **никогда** не хранит цены и объёмы в `float`/`double`. Причина — деньги: у double всего ~16 значащих десятичных цифр, и две арифметические операции могут дать `0.1 + 0.2 = 0.30000000000000004`. Для сведения заявок и подсчёта баланса это недопустимо: нельзя сравнивать цены через `==`, нельзя накапливать суммы без дрейфа.

Решение, которое используют все биржи: **fixed-point — хранить целое число минимальных шагов.**

### 4.2. Что такое tick и step

Биржа для каждого символа задаёт:
- **`tickSize`** — минимальный шаг цены. Любая допустимая цена кратна `tickSize`.
- **`stepSize`** — минимальный шаг объёма. Любой допустимый объём кратен `stepSize`.

У нас они уже есть в `SymbolInfo`:
```kotlin
data class SymbolInfo(
    val tickSize: Double,   // напр. BTCUSDT futures = 0.10
    val stepSize: Double,   // напр. = 0.001
    ...
)
```

### 4.3. Идея: вместо дроби храним «сколько шагов»

```
priceTicks = round(price / tickSize)   // целое (Long)
qtySteps   = round(qty   / stepSize)   // целое (Long)
```

Цена/объём для отображения восстанавливаются обратно:
```
price = priceTicks * tickSize
qty   = qtySteps   * stepSize
```

**Это и есть «как думает матчинг-движок».** Мы строим локальный мини-стакан в той же системе координат, что и биржа.

### 4.4. Числовые примеры

**BTCUSDT, tickSize = 0.10, stepSize = 0.001:**

| Цена (строка с биржи) | priceTicks | Объём | qtySteps |
|---|---|---|---|
| `67234.50` | `672345` | `1.234` | `1234` |
| `67234.60` | `672346` | `0.001` | `1` |
| `67234.40` | `672344` | `15.000` | `15000` |

Соседние цены → соседние целые. Точно. После парсинга — никакого float.

**Щиткоин, tickSize = 0.000000001 (1e-9), stepSize = 0.000000000001 (1e-12):**

| Цена | priceTicks | Объём | qtySteps |
|---|---|---|---|
| `0.000000012` | `12` | `0.000000000012` | `12` |
| `0.000000013` | `13` | `1.000000000000` | `1000000000000` |

Маленькие, точные целые. **Сатоши не теряются** — мы храним ровно «сколько минимальных единиц», как сама биржа.

### 4.5. Почему это решает все три FP-проблемы

| Проблема double | Решение fixed-point |
|---|---|
| **(a)** равенство/ключи после арифметики (`aggregationKey` округление) | ключ — `Long`. Точное сравнение и хэш, идеальная сортировка |
| **(b)** дрейф при суммировании объёмов в бакете | `bucketSteps += deltaSteps` — сложение `Long`, **дрейфа нет вообще** |
| **(c)** наивный `toString()` в отображении | форматируем `priceTicks*tickSize` через `SymbolFormatter` по известным decimals |

### 4.6. Агрегация = целочисленное деление

Уровень агрегации — множитель `M` (1x, 10x, 100x). Бакет, в который попадает уровень:

```
bucketIndex = Math.floorDiv(priceTicks, M)     // целочисленное деление вниз
```

Все сырые уровни с одинаковым `bucketIndex` суммируются в одну строку. Репрезентативная цена бакета:

```
bucketPriceTicks = bucketIndex * M
```

**Пример BTCUSDT 10x (M=10), tickSize=0.10 (то есть шаг бакета = 1.0):**

```
price 67234.50 → priceTicks 672345 → bucketIndex floorDiv(672345,10)=67234 → bucketPriceTicks 672340 → цена 67234.0
price 67234.60 → priceTicks 672346 → bucketIndex 67234 → тот же бакет 67234.0
price 67234.40 → priceTicks 672344 → bucketIndex 67234 → тот же бакет 67234.0
price 67235.10 → priceTicks 672351 → bucketIndex 67235 → бакет 67235.0
```

Три цены [67234.40, 67234.50, 67234.60] схлопнулись в один бакет `67234.0`, их объёмы (в steps) сложились точно. `floorDiv` (вниз) совпадает с текущим поведением `aggregationKey.roundDown` для обеих сторон.

### 4.7. Дельта-трюк (потому что diff Binance — абсолютный)

Binance шлёт **абсолютный** объём на цене (`[price, newQty]`, `qty=0` = удалить), не дельту. Чтобы инкрементально вести бакет, дельту считаем сами, держа сырую карту:

```
oldSteps   = rawMap[priceTicks] ?: 0
deltaSteps = newSteps - oldSteps           // Long
rawMap[priceTicks] = newSteps              // (или remove, если newSteps == 0)
bucket[bucketIndex].bidSteps += deltaSteps // точное сложение Long
// если все объёмы бакета == 0 → удалить бакет (и строку)
```

Дельта-формула одинаково покрывает add / update / remove. Стоимость: O(1) на апдейт + O(log K) на патч строки в отсортированном списке.

---

## 5. Как применяем к нашему DOM

Конвейер в новых координатах:

1. **Граница парсинга** (репозиторий уже даёт `DomEvent.UpdateBid/Ask(price: Double, quantity: Double)`): во ViewModel переводим в целые
   `priceTicks = round(price/tickSize)`, `qtySteps = round(qty/stepSize)`.
2. **Сырьё (источник истины)**: `rawBids: LongLongMap` (priceTicks → qtySteps), `rawAsks: LongLongMap`. Нужны для дельты.
3. **Агрегированные бакеты**: `bidBuckets: HashMap<Long, Long>` (bucketIndex → суммаSteps), `askBuckets`. Ведём дельтой.
4. **Отображаемый список**: персистентный `SnapshotStateList<DomLevel>` — отсортирован по убыванию `bucketPriceTicks`, усечён по `depth` (правило B), патчится точечно.
5. **UI**: `LazyColumn` читает список напрямую, `key = priceTicks` (Long, без String-аллокаций), `LevelRow` форматирует `priceTicks*tickSize` / `qtySteps*stepSize` только для видимых строк.

Конвертация в Double происходит **только** при форматировании видимых строк (≤ ~2·depth штук), а не по всей книге.

---

## 6. Целевой поток данных

**Было:**
```
SnapshotStateMap<Double,Double> → derivedStateOf{ rebuild ALL } → new List → LazyColumn
                                   (O(N) + аллокации каждый тик)
```

**Стало:**
```
DomEvent → VM: rawMap(Long→Long) + buckets(Long→Long), патч дельтой
        → persistent SnapshotStateList<DomLevel>  (точечный set/add/removeAt)
        → LazyColumn items(key = priceTicks) { LevelRow }   (рекомпозиция только изменившихся строк)
```

---

## 7. Модель данных: DomLevel

Новый internal-тип в `feature-dom` (commonMain), напр. `ui/model/DomLevel.kt`:

```kotlin
import androidx.compose.runtime.Immutable

/**
 * Уровень стакана в координатах матчинг-движка (fixed-point).
 * priceTicks  — цена как число тиков (priceTicks * tickSize = цена)
 * bidSteps    — объём bid как число степов (bidSteps * stepSize = объём)
 * askSteps    — объём ask как число степов
 * 0 в объёме означает «нет этой стороны на уровне».
 */
@Immutable
internal data class DomLevel(
    val priceTicks: Long,
    val bidSteps: Long,
    val askSteps: Long,
)
```

- `@Immutable` → Compose скипает рекомпозицию ряда, если объект `equals` предыдущему.
- Только примитивы → ноль аллокаций строк, идеальная стабильность для Compose.
- `OrderBookLevel` (public-api, String) остаётся для совместимости, в горячем пути не участвует.

---

## 8. ViewModel: инкрементальная агрегация дельтой

### 8.1. Состояние

```kotlin
// Масштаб символа (загружается из SymbolInfo)
private var tickSize: Double = 0.0
private var stepSize: Double = 0.0
private var scaleReady: Boolean = false

// Сырьё (источник истины) — обычные не-Snapshot карты, читателей нет
private val rawBids = HashMap<Long, Long>()   // priceTicks -> qtySteps
private val rawAsks = HashMap<Long, Long>()

// Агрегированные бакеты (по текущему множителю агрегации)
private val bidBuckets = HashMap<Long, Long>() // bucketIndex -> суммаSteps
private val askBuckets = HashMap<Long, Long>()

// Отображаемый список (то, что читает Compose)
private val _displayLevels = mutableStateListOf<DomLevel>()
val displayLevels: List<DomLevel> = _displayLevels

// Лучшие цены в тиках (для усечения и подсветки)
private var bestBidTicks: Long? = null
private var bestAskTicks: Long? = null
```

### 8.2. Обработка событий

```kotlin
private fun onUpdate(priceTicks: Long, newSteps: Long, side: Side) {
    val raw = if (side == BID) rawBids else rawAsks
    val buckets = if (side == BID) bidBuckets else askBuckets

    val oldSteps = raw[priceTicks] ?: 0L
    val delta = newSteps - oldSteps
    if (delta == 0L) return

    if (newSteps == 0L) raw.remove(priceTicks) else raw[priceTicks] = newSteps

    val bucketIndex = Math.floorDiv(priceTicks, aggMultiplier)
    val newBucketSteps = (buckets[bucketIndex] ?: 0L) + delta
    if (newBucketSteps <= 0L) buckets.remove(bucketIndex) else buckets[bucketIndex] = newBucketSteps

    patchDisplayLevel(bucketIndex, side)   // точечно обновить/добавить/удалить строку
}
```

`aggMultiplier` = 1 / 10 / 100 в зависимости от `domOptions.aggregation`.

### 8.3. Патч отображаемого списка

`_displayLevels` отсортирован по убыванию `priceTicks` (бакетной цены). Патчим точечно:

```kotlin
private fun patchDisplayLevel(bucketIndex: Long, side: Side) {
    val priceTicks = bucketIndex * aggMultiplier
    val bid = bidBuckets[bucketIndex] ?: 0L
    val ask = askBuckets[bucketIndex] ?: 0L

    val idx = binarySearchByDesc(_displayLevels, priceTicks) { it.priceTicks }
    if (bid == 0L && ask == 0L) {
        if (idx >= 0) _displayLevels.removeAt(idx)        // строка опустела
        return
    }
    val level = DomLevel(priceTicks, bid, ask)
    if (idx >= 0) _displayLevels[idx] = level             // обновить (Compose дернёт только этот ряд)
    else _displayLevels.add(-idx - 1, level)              // вставить в правильную позицию
    enforceDepth()                                        // усечение (см. §9)
}
```

`binarySearchByDesc` — поиск по убыванию: O(log K), K ≤ 2·depth.

### 8.4. Снапшот / Reset — полный resync

На `DomEvent.Snapshot` и `DomEvent.Reset` чистим всё и пересобираем из снапшота. Это:
- даёт корректную инициализацию,
- служит **точкой ресинхронизации** (хотя при Long-сложении дрейфа нет, resync убирает любые рассинхроны протокола).

### 8.5. Смена уровня агрегации

При смене `aggregation` (1x↔10x↔100x) сырьё не меняется — пересобираем только бакеты:
```kotlin
private fun rebuildBucketsFromRaw() {
    bidBuckets.clear(); askBuckets.clear()
    rawBids.forEach { (pt, steps) -> bidBuckets.merge(floorDiv(pt, aggMultiplier), steps, Long::plus) }
    rawAsks.forEach { (pt, steps) -> askBuckets.merge(floorDiv(pt, aggMultiplier), steps, Long::plus) }
    rebuildDisplayFromBuckets()
}
```
Это происходит редко (по действию пользователя), поэтому полный проход допустим.

### 8.6. maxVolume

Для баров объёма нужен максимум среди видимых. Считаем из `_displayLevels` (≤ ~2·depth строк) — тривиально, либо отдаём как производное на уровне UI из того же списка. Никаких проходов по всей книге.

---

## 9. depth = B: правило усечения

`depth` = число агрегированных **строк отображения на сторону**.

- **bids**: оставляем `depth` бакетов с наибольшим `priceTicks` среди `priceTicks <= bestBidBucket`.
- **asks**: оставляем `depth` бакетов с наименьшим `priceTicks` среди `priceTicks >= bestAskBucket`.
- Итого в `_displayLevels` ~ `2·depth` строк, окно вокруг спреда.

`enforceDepth()` после вставки удаляет дальние от спреда лишние строки (с краёв списка). Сырьё (`rawBids/rawAsks`) при этом сохраняем полностью — оно нужно, чтобы при движении best-цены поднять в окно ранее усечённые уровни.

> Пример: depth=100, агрегация 10x → 100 бид-строк + 100 аск-строк, каждая покрывает 10 тиков → видимое окно ~ 1000 тиков на сторону. Именно «глубокий вид».

---

## 10. UI: DomWindow / DomSection / LevelRow

### DomWindow
- Удалить `buildDisplayOrderBook` и `derivedStateOf`-ребилд.
- Читать `vm.displayLevels`, `vm.tickSize`, `vm.stepSize`, best-цены (Long/Double) напрямую.

### DomSection
```kotlin
LazyColumn(state = lazyListState) {
    items(
        items = vm.displayLevels,
        key = { it.priceTicks }           // Long-ключ, без String-аллокации
    ) { level ->
        LevelRow(level = level, tickSize = tickSize, stepSize = stepSize,
                 maxSteps = maxSteps, bestBidTicks = bestBidTicks, bestAskTicks = bestAskTicks,
                 selectedTicks = selectedTicks, onClick = …)
    }
}
```
- Авто-скролл к best: индекс ищем в маленьком `displayLevels` сравнением `Long` (`priceTicks == bestBidBucketTicks`). Убрать `aggregationKey` и `remember(orderBook,…)`.

### LevelRow
- Принимает `DomLevel` + масштаб; читает целые поля напрямую.
- `price = priceTicks * tickSize` → `formatPrice`; `qty = steps * stepSize` → `formatVolume` (только для видимой строки).
- Подсветка selected/best — сравнение `Long` (`level.priceTicks == selectedTicks` и т.п.). Ноль `toDoubleOrNull`, ноль `aggregationKey`.
- Ширина бара объёма: `steps.toFloat() / maxSteps.toFloat()`.

---

## 11. Правильная рекомпозиция

Три механизма вместе дают рекомпозицию только изменившихся строк:

1. **`@Immutable DomLevel`** → skippable composable: ряд с теми же параметрами не перерисовывается.
2. **Стабильный `key = priceTicks`** (Long) → слот LazyColumn сохраняется по ключу; обновлённый ряд перерисовывается, остальные пропускаются; сдвиг → перемещение слота, а не пересборка.
3. **Персистентный `SnapshotStateList`, мутируемый in-place** (`set/add/removeAt`) → нет апстрим-ребилда; Compose диффит на уровне элементов. Это ключ: сейчас главная потеря — сам `derivedStateOf`-ребилд ещё до диффа рядов.

---

## 12. Граничные случаи

| Случай | Решение |
|---|---|
| Масштаб (tick/step) ещё не загружен | До загрузки `SymbolInfo` буферизуем/не строим список; снапшот приходит ~через 500мс после подписки. Фолбэк: вывести decimals из строк снапшота, если API не дал |
| Объём строки с бОльшим числом знаков, чем step | `round(qty/stepSize)` — округление к ближайшему step (Binance и так шлёт кратное) |
| Парсинг строки → scaled Long | `Math.round(value / step)`; деление double безопасно: step ≫ epsilon double на этих величинах, округление к целому точное. (Опц. хардненинг — точный парсинг десятичной строки сдвигом точки) |
| Переполнение Long | priceTicks ≈ цена/tick ≤ ~1e15 ≪ Long.MAX (9.2e18). Безопасно |
| `qty = 0` (удаление) | `delta = 0 - old`, bucket уменьшается, при нуле бакет и строка удаляются |
| Округление бакета bid vs ask | `floorDiv` (вниз) для обеих сторон — совпадает с текущим `roundDown` |

---

## 13. Пошаговый план реализации

- **Фаза 0** — `DomViewModel`: загрузка `stepSize` рядом с `tickSize` (через `SymbolInfoRepository`), экспонировать `tickSize/stepSize`, флаг `scaleReady`.
- **Фаза 1** — модель `DomLevel` (scaled-Long, `@Immutable`) + утилита `binarySearchByDesc`.
- **Фаза 2** — `DomViewModel`: `rawBids/rawAsks`, `bidBuckets/askBuckets`, `_displayLevels`; `onUpdate` дельтой, `patchDisplayLevel`, `enforceDepth`, resync на Snapshot/Reset, `rebuildBucketsFromRaw` на смене агрегации, `maxSteps`.
- **Фаза 3** — `DomWindow`: убрать `buildDisplayOrderBook`/`derivedStateOf`, читать `displayLevels`.
- **Фаза 4** — `DomSection`/`LevelRow`: `items(key=priceTicks)`, Double-free ряды, скролл/подсветка по Long.
- **Фаза 5** — почистить ставшие ненужными String-пути в `feature-dom` (`OrderBook`, `DomAggregator`), если их больше не использует UI. Public-api не трогаем.
- **Фаза 6** — тесты + ручной прогон 12-грида + профайлер до/после.

---

## 14. Тесты

- **Масштабирование (round-trip)**: `"67234.50"` → `672345` → `"67234.50"`; щиткоин-кейсы.
- **Дельта-агрегация**: add / update / remove, проверка точной суммы бакета; удаление бакета при нуле.
- **Смена уровня агрегации**: `rebuildBucketsFromRaw` даёт те же суммы, что и пересбор с нуля.
- **Усечение depth=B**: ровно `depth` строк на сторону; при движении best-цены ранее усечённые уровни возвращаются.
- **Точность**: после миллиона апдейтов сумма бакета == прямой сумме сырья (нулевой дрейф — Long).
- Обновить `DomViewModelTest`, `DomRepositoryImplTest`.
- **Ручной**: грид `Templates.domGrid` → плавность; профайлер (async-profiler / IntelliJ) — `buildDisplayOrderBook` должен исчезнуть из flame graph.

---

## 15. Риски и митигации

| Риск | Митигация |
|---|---|
| Масштаб не загрузился / символ без `SymbolInfo` | флаг `scaleReady`, фолбэк-инференс decimals из строк, list пустой до готовности |
| Рассинхрон бакетов и сырья | единственная точка изменения бакетов — `onUpdate`/`rebuildBucketsFromRaw`; resync на Snapshot/Reset |
| Поведение агрегации изменилось vs текущее | `floorDiv` повторяет `roundDown`; добавить тест-сравнение со старым `DomAggregator` на фикстуре |
| Регресс точности ордера | путь ордера не трогаем (уже Double); опц. хардненинг отдельной задачей |

---

## 16. Что НЕ трогаем / отложено

- **Источник данных** (diff stream, `OrderBookState`, протокол синхронизации) — без изменений.
- **`OrderBookLevel`** (public-api, String) — без изменений (бинарная совместимость).
- **Путь ордера** — уже Double; точный scaled-Long до команды — опционально, позже.
- **Lifecycle DOM** — уже закрыт в `61d910c`.
- **Мелочи горячего пути** (`LaunchedEffect` авто-скролла) — оптимизируются попутно в Фазе 4.
