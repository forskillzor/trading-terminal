# План: Завершение scroll/zoom фиксов + обновление документации

## Статус-кво

### ✅ УЖЕ СДЕЛАНО (scroll/zoom fixes в ChartInteraction.kt)

| Фича | Статус |
|------|--------|
| `maxScroll` как состояние (не `Float.MAX_VALUE`) | ✅ |
| Drag ограничен `coerceIn(-maxScrollLeft, maxScroll)` | ✅ |
| При новой загрузке — `scrollOffset = maxScroll` (правый край) | ✅ |
| `clampedOffset` с корректным клиппингом | ✅ |
| `clickable(indication=null)` для focusability (`onKeyEvent`) | ✅ |
| `onKeyEvent` для Ctrl | ✅ |
| Зум без Ctrl — фиксация правого края | ✅ |
| Зум с Ctrl — фиксация под курсором | ✅ |
| Zoom limits `0.25f..4.0f` | ✅ |
| Dynamic PriceRange по видимым свечам | ✅ |
| Коррекция scrollOffset после prepend истории | ✅ |

### ✅ УЖЕ СДЕЛАНО (рефакторинг — 13 файлов, 4 пакета)

- `model/`: PriceRange.kt, CandleMetrics.kt, ChartLayout.kt
- `utils/`: ChartConstants.kt, ChartCalculator.kt, Format.kt
- `rendering/`: CandleRenderer.kt, ChartPriceScaleRenderer.kt, ChartTimeScaleRenderer.kt, ChartCrosshairRenderer.kt, ChartTextRenderer.kt
- `ui/chart/`: CandleStickChart.kt (тонкая обёртка), ChartInteraction.kt (вся логика)
- Старый CandleStickChartWidget.kt удалён
- BUILD SUCCESSFUL (feature-chart + composeApp)

### ❌ ОСТАЛОСЬ СДЕЛАТЬ

1. **Проверить билд** — убедиться, что код всё ещё собирается
2. **Обновить `docs/feature-chart-technical-book.md`** — книга описывает старую структуру

## Порядок выполнения

### Шаг 1: Проверка билда
```bash
./gradlew :features:feature-chart:compileKotlinJvm :composeApp:compileKotlinJvm
```

### Шаг 2: Обновление технической книги docs/feature-chart-technical-book.md

#### 2.1 Раздел 1.4 — Структура файлов
Заменить старую структуру (1 файл CandleStickChartWidget.kt) на новую (13 файлов, 4 пакета):

```
features/feature-chart/
├── build.gradle.kts
└── src/commonMain/kotlin/com/aandios/nous/feature/chart/
    ├── di/
    │   └── FeatureChartModule.kt       # Koin DI модуль
    ├── model/                          # Модели данных (SRP)
    │   ├── PriceRange.kt               # Диапазон цен
    │   ├── CandleMetrics.kt            # Метрики свечи (ширина, отступ)
    │   └── ChartLayout.kt             # Компоновка областей графика
    ├── rendering/                      # Функции отрисовки Canvas (SRP)
    │   ├── CandleRenderer.kt          # Свечи, сетка, линия цены
    │   ├── ChartPriceScaleRenderer.kt # Шкала цен
    │   ├── ChartTimeScaleRenderer.kt  # Шкала времени
    │   ├── ChartCrosshairRenderer.kt  # Перекрестие
    │   └── ChartTextRenderer.kt       # Утилита текста
    ├── ui/
    │   ├── chart/
    │   │   ├── CandleStickChart.kt    # Тонкая обёртка (20 строк)
    │   │   └── ChartInteraction.kt    # Вся интерактивная логика (~14KB)
    │   ├── ChartConfig.kt            # Конфигурация отрисовки
    │   ├── ChartToolbar.kt           # Панель инструментов
    │   ├── ChartViewModel.kt         # ViewModel с бизнес-логикой
    │   └── ChartWindow.kt           # Точка входа
    └── utils/
        ├── ChartConstants.kt         # Константы (BASE_CANDLE_WIDTH)
        ├── ChartCalculator.kt        # Чистые функции расчёта
        └── Format.kt                # Форматирование цен и времени
```

#### 2.2 Раздел 7 — CandleStickChart
Переписать: CandleStickChart — теперь тонкая обёртка (20 строк, только @Composable, делегирует CandleStickChartInteraction). CandleStickChartInteraction содержит всю логику: состояния, жесты, layout, Canvas.

#### 2.3 Раздел 8 — ChartLayout
Обновить импорт: `com.aandios.nous.feature.chart.model.ChartLayout`

#### 2.4 Раздел 9 — Canvas-рендеринг
Функции рисования вынесены в rendering/ пакет. Вместо private функций DrawScope в CandleStickChartWidget — extension-функции из отдельных файлов:
- `drawChart()` → `CandleRenderer.kt`
- `drawTimeScale()` → `ChartTimeScaleRenderer.kt`
- `drawPriceScale()` → `ChartPriceScaleRenderer.kt`
- `drawCrosshair()` → `ChartCrosshairRenderer.kt`
- `drawTextLine()` → `ChartTextRenderer.kt`

#### 2.5 Раздел 10-11 — Сетка, шкалы
Обновить ссылки на rendering/ пакет.

#### 2.6 Раздел 12 — Система скролла
Описать исправления:
- `maxScroll` как `mutableFloatStateOf(0f)` вместо вычисления на лету
- `coerceIn(-maxScrollLeft, maxScroll)` — корректное ограничение справа
- `LaunchedEffect` для начального скролла к правому краю
- Коррекция `scrollOffset += added` после prepend исторических свечей

#### 2.7 Раздел 13 — Система зума
Описать:
- `Modifier.clickable(indication = null)` для focusability
- `onKeyEvent` для Ctrl
- Зум без Ctrl → фиксация правого края
- Зум с Ctrl → фиксация под курсором
- `coerceIn(0.25f, 4.0f)` — границы зума

#### 2.8 Раздел 14 — Dynamic PriceRange
- `visibleCandles` через `candles.subList(startIdx, endIdx)` по видимым свечам
- `priceRange = remember(visibleCandles, currentPrice)`

#### 2.9 Новая диаграмма архитектуры
Добавить Mermaid-диаграмму, показывающую 13 файлов, связи, потоки вызовов
