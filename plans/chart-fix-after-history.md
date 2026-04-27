# План: Доработка фич после истории

## Контекст

Файл уже содержит исправленный `calculateCandleMetrics(zoomLevel)` с `BASE_CANDLE_WIDTH = 8f`:
- `calculateCandleMetrics(zoomLevel)` — ширина свечи = `8px * zoomLevel`
- `candleMetrics = remember(zoomLevel)` — не зависит от candleCount
- `maxScroll = max(0f, candles.size * totalW - chartWidthPx)` — корректный (зависит от candleCount через totalW)

## Задача A: Zoom limits

**Сейчас:** `coerceIn(0.3f, 20.0f)` — слишком большой диапазон (20x зум).

**Нужно:** максимальное увеличение в 4 раза, минимальное уменьшение в 4 раза.

→ Изменить 141 строку в `CandleStickChartWidget.kt`:

```kotlin
// Было (строка 141):
val newZoom = (oldZoom * factor).coerceIn(0.3f, 20.0f)

// Стало:
val newZoom = (oldZoom * factor).coerceIn(0.25f, 4.0f)
```

## Задача B: Dynamic price range по видимым свечам

**Сейчас:** `priceRange` считается по ВСЕМ свечам (строка 90, до BoxWithConstraints).

**Нужно:** при зуме/панарамировании `priceRange` считается только по ВИДИМЫМ свечам (startIdx..endIdx), чтобы график всегда занимал максимум по высоте.

### Изменения

**1. Удалить строки 89–92** (старый priceRange):
```kotlin
    // Расчет минимальной и максимальной цены
    val priceRange = remember(candles, currentPrice) {
        calculatePriceRangeWithCurrentPrice(candles, currentPrice)
    }
```

**2. Вставить новый priceRange ПОСЛЕ endIdx** (после строки 244):
```kotlin
    // Price range ТОЛЬКО по видимым свечам — автоподстройка Y при зуме/панарамировании
    val visibleCandles = remember(startIdx, endIdx) {
        candles.subList(startIdx, endIdx.coerceAtMost(candles.size))
    }
    val priceRange = remember(visibleCandles, currentPrice) {
        calculatePriceRangeWithCurrentPrice(visibleCandles, currentPrice)
    }
```

**3. Импорт** — `subList` уже есть из `kotlin.collections`, ничего добавлять не нужно.

### Как это работает

- `startIdx`/`endIdx` зависят от `scrollOffset`, `zoomLevel`, `chartWidthPx`
- `visibleCandles` пересчитывается при изменении индексов (зум/панарамирование)
- `priceRange` пересчитывается только для этих свечей → Y-ось масштабируется под видимый диапазон
- При загрузке новых данных `candles` меняется, но `subList` по тем же индексам возвращает новые свечи

## Итоговый порядок применения

1. Сначала примени задачу A (строка 141: `0.25f..4.0f`)
2. Потом задачу B (удалить priceRange в начале, добавить после endIdx)
3. Собрать: `./gradlew :features:feature-chart:compileKotlinJvm`
4. Запустить: `./gradlew :features:feature-chart:run`
