# План: Исправление багов после загрузки истории

## Баг 1: LaunchedEffect(timestamp) перетирает scrollOffset

**Файл:** `CandleStickChartWidget.kt:231-233`

**Проблема:**
```kotlin
LaunchedEffect(candles.firstOrNull()?.timestamp ?: 0L) {
    scrollOffset = maxScroll
}
```

После prepend исторических свечей `candles.firstOrNull()?.timestamp` меняется (первая свеча теперь старше). LaunchedEffect срабатывает снова и устанавливает `scrollOffset = maxScroll` — крайнее правое положение. Это **перетирает** коррекцию из `LaunchedEffect(historyLoadCount, candles.size)`.

**Последовательность:**
1. Пользователь тянет влево → scrollOffset < 0 → onNeedMoreHistory()
2. loadMoreHistory() добавляет 200 свечей → candles = 400
3. LaunchedEffect(historyLoadCount=200, candles.size=400) → scrollOffset += 200 * totalW ✅
4. LaunchedEffect(timestamp=newOldest) → scrollOffset = maxScroll ❌ — прыжок направо!

**Фикс:** Добавить guard `if (historyLoadCount == 0)`, чтобы LaunchedEffect срабатывал только при смене символа/таймфрейма, а не после prepend истории.

---

## Баг 2: Zoom без Ctrl — прыгает

**Файл:** `CandleStickChartWidget.kt:132-155`

**Текущее поведение:** Zoom всегда относительно свечи под курсором.

**Требование:**
- **Ctrl + scroll** — zoom относительно свечи под курсором (keep cursor stationary)
- **scroll (без Ctrl)** — zoom относительно последней (самой новой, правой) свечи (keep rightmost stationary)

**Формулы:**

Для удержания правой свечи неподвижной:
```
rightmostVirtual = (candles.size - 1) * totalW
newScrollOffset = rightmostVirtual * (actualFactor - 1) + scrollOffset
```

Для удержания свечи под курсором (как сейчас):
```
virtualPos = mouseX + scrollOffset
newScrollOffset = virtualPos * actualFactor - mouseX
```

**Детекция Ctrl:** `event.keyboardModifiers.isCtrlPressed` на `PointerEvent`

---

## Изменения в коде

### 1. CandleStickChartWidget.kt:231-233 — guard на LaunchedEffect

```kotlin
// Было:
LaunchedEffect(candles.firstOrNull()?.timestamp ?: 0L) {
    scrollOffset = maxScroll
}

// Стало:
LaunchedEffect(candles.firstOrNull()?.timestamp ?: 0L) {
    if (historyLoadCount == 0) {
        scrollOffset = maxScroll
    }
}
```

### 2. CandleStickChartWidget.kt:132-155 — zoom с поддержкой Ctrl

```kotlin
.pointerInput(Unit) {
    awaitPointerEventScope {
        while (true) {
            val event = awaitPointerEvent()
            val change = event.changes.firstOrNull() ?: continue
            val sd = change.scrollDelta
            if (event.type == PointerEventType.Scroll && sd != Offset.Zero) {
                val factor = if (sd.y < 0) 1.15f else 1f / 1.15f
                val oldZoom = zoomLevel
                val newZoom = (oldZoom * factor).coerceIn(0.3f, 5.0f)
                val actualFactor = newZoom / oldZoom

                val mouseX = change.position.x
                val ctrlHeld = event.keyboardModifiers.isCtrlPressed

                val newScrollOffset = if (ctrlHeld) {
                    // Ctrl+scroll: keep candle under cursor stationary
                    val virtualPos = mouseX + scrollOffset
                    virtualPos * actualFactor - mouseX
                } else {
                    // Default scroll: keep rightmost (newest) candle stationary
                    val rightmostVirtual = (candles.size - 1) * totalW
                    rightmostVirtual * (actualFactor - 1) + scrollOffset
                }

                zoomLevel = newZoom
                scrollOffset = newScrollOffset.coerceIn(-maxScrollLeft, Float.MAX_VALUE)
                change.consume()
            }
        }
    }
}
```

**Важно:** `candles` и `totalW` доступны внутри `.pointerInput(Unit)`, но могут устареть. `totalW` — это константа на основе candleWidth + candleGap. `candles.size` может измениться. Для корректного захвата используем `remember(candles.size, candleWidth, candleGap)` или просто захватываем свежие значения через `currentComposition`.

Лучше использовать:
```kotlin
val candlesSize = candles.size
```
в композиции, а внутри pointerInput захватить эту переменную. Или использовать `remember` для totalW и candles.size, чтобы pointerInput имел свежие значения.

На практике `candles.size` как `remember` переменная снаружи, а внутри pointerInput используется `candlesSize` или `totalW`.

Фактически, проще всего вынести candles.size в локальную переменную перед pointerInput.

### 3. CandleStickChartWidget.kt — добавить импорт `isCtrlPressed`

```kotlin
import androidx.compose.ui.input.pointer.PointerEventType
```
Уже есть. Нужен только `keyboardModifiers` — он доступен на `PointerEvent` без дополнительного импорта.
