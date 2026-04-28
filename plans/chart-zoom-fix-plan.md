# План: Исправление зума и "прилипания" графика

## Проблемы

### 1. Зум всегда относительно курсора (не соответствует спецификации)
Текущий код (строки 139-145) всегда считает `virtualPos = mouseX + scrollOffset`. Это зум относительно курсора. По спецификации нужно:
- **Без Ctrl** (по умолчанию): зум относительно **правой видимой свечи** (самой новой по времени на экране)
- **С Ctrl**: зум относительно **свечи под курсором мыши**

### 2. "Прилипание" к правому краю
Когда `scrollOffset == maxScroll` (крайнее правое положение), любой зум даёт `newScrollOffset > maxScroll`, но `coerceIn(-maxScrollLeft, Float.MAX_VALUE)` затем `clampedOffset = coerceIn(-maxScrollLeft, maxScroll)` — это обрезает scrollOffset обратно к maxScroll. Создаётся эффект "прилипания": зум и скролл не могут оторваться от правого края.

## Решение

### Файл: [`CandleStickChartWidget.kt`](features/feature-chart/src/commonMain/kotlin/com/aandios/nous/feature/chart/ui/CandleStickChartWidget.kt)

#### 1. Добавить `chartWidthPx` как state (строка ~86)
`chartWidthPx = layout.chartMainArea.width` определён внутри `BoxWithConstraints` и недоступен в `pointerInput` для зума. Выносим в `remember { mutableFloatStateOf(0f) }`.

#### 2. Исправить логику зума (строки 127-151)
- Проверять `change.keyboardModifiers.isCtrlPressed` / `isMetaPressed`
- **Без Ctrl**: `newScrollOffset = (scrollOffset + chartWidthPx) * actualFactor - chartWidthPx`
  — фиксирует правый край графика (самую новую свечу по времени)
- **С Ctrl**: `newScrollOffset = (mouseX + scrollOffset) * actualFactor - mouseX`
  — фиксирует свечу под курсором

#### 3. Убрать `coerceIn` из scrollOffset в зуме
Убрать `.coerceIn(-maxScrollLeft, Float.MAX_VALUE)` при присвоении `scrollOffset` внутри зума. `clampedOffset` на строчке 235 уже корректно клиппит отображение. Без этого коэрцина зум не будет "прилипать" к правому краю.

## Проверка
1. Запустить `./gradlew :features:feature-chart:run`
2. Проверить зум колёсиком (без Ctrl): правый край графика остаётся неподвижным
3. Проверить зум с Ctrl: свеча под курсором остаётся неподвижной
4. Проверить, что можно свободно скроллить влево-вправо без "прилипания"
