# Диагностика Ctrl-зума

## Проблема

Ctrl-зум не срабатывает: зум всегда работает от правого края (как TradingView), независимо от нажатия Ctrl.

## Анализ кода

В [`CandleStickChartWidget.kt`](features/feature-chart/src/commonMain/kotlin/com/aandios/nous/feature/chart/ui/CandleStickChartWidget.kt:95) определена переменная:

```kotlin
var isCtrlPressed by remember { mutableStateOf(false) }
```

На строках 107-114 она должна обновляться через `Modifier.onKeyEvent`:

```kotlin
.onKeyEvent { event ->
    if (event.key == Key.CtrlLeft || event.key == Key.CtrlRight) {
        isCtrlPressed = event.type == KeyEventType.KeyDown
        true
    } else {
        false
    }
}
```

На строке 158 она используется для выбора режима зума:

```kotlin
val newScrollOffset = if (isCtrlPressed) {
    // Ctrl+zoom: фиксируем свечу под курсором
    ...
} else {
    // Обычный зум: фиксируем правый край
    ...
}
```

### Корневая причина

**`Modifier.onKeyEvent` не вызывается в runtime**, потому что [`BoxWithConstraints`](features/feature-chart/src/commonMain/kotlin/com/aandios/nous/feature/chart/ui/CandleStickChartWidget.kt:103) **не является focusable** composable.

В Compose Multiplatform, клавиатурные события (`onKeyEvent`, `onPreviewKeyEvent`) доставляются только composable, которые находятся в фокусе. `BoxWithConstraints` по умолчанию не может получить фокус, поэтому блок `onKeyEvent { }` никогда не выполняется.

**Следствие:**
- `isCtrlPressed` всегда остаётся `false`
- Зум всегда работает от правого края (ветка `else`)
- Ctrl-зум никогда не срабатывает

## Решения

### Вариант A (рекомендуемый): `Modifier.clickable(indication = null) {}`

Добавить `Modifier.clickable` перед `onKeyEvent`, чтобы сделать composable focusable без визуальных изменений:

```kotlin
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource

BoxWithConstraints(
    modifier = modifier
        .fillMaxSize()
        .background(config.backgroundColor)
        .clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null
        ) { /* no-op: must be focusable for onKeyEvent */ }
        .onKeyEvent { event -> ... }
)
```

**Плюсы:**
- Минимальные изменения (только в одном файле)
- Не нужны experimental API
- `clickable` есть в `compose.foundation` (транзитивно через material3)
- Не конфликтует с `pointerInput` для zoom/pan (там drag и scroll, не tap)

### Вариант B: `Modifier.focusProperties { canFocus = true }`

```kotlin
import androidx.compose.ui.focus.focusProperties

.focusProperties { canFocus = true }
```

**Проблема:** `focusProperties` — `@ExperimentalFocus` API. Может не компилироваться в commonMain без `@OptIn`.

### Вариант C: Перенос на уровень `ChartWindow`

Поднять `onKeyEvent` в [`ChartWindow.kt`](features/feature-chart/src/commonMain/kotlin/com/aandios/nous/feature/chart/ui/ChartWindow.kt), передавать `isCtrlPressed` через параметр.

**Проблема:** Требует изменения сигнатуры `CandleStickChart()` и `ChartWindow()`. Более инвазивно.

## План исправления

1. В [`CandleStickChartWidget.kt`](features/feature-chart/src/commonMain/kotlin/com/aandios/nous/feature/chart/ui/CandleStickChartWidget.kt) добавить импорты:
   - `import androidx.compose.foundation.clickable`
   - `import androidx.compose.foundation.interaction.MutableInteractionSource`

2. В modifier chain после `.background()` добавить:
   ```kotlin
   .clickable(
       interactionSource = remember { MutableInteractionSource() },
       indication = null
   ) { }
   ```

3. Собрать проект и проверить:
   - Зум из правого края (без Ctrl) — работает как раньше
   - Зум от свечи под курсором (с Ctrl) — должно начать работать

## Верификация

После исправления:
- `isCtrlPressed = true` когда зажат Ctrl → зум от позиции курсора
- `isCtrlPressed = false` когда Ctrl не зажат → зум от правого края
- Никаких изменений в поведении пана, кроссхеира или скролла
