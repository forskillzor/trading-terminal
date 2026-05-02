# План миграции composeApp на новые feature-модули

## Текущая проблема

После рефакторинга монолита `composeApp` были вынесены feature-модули:
- `features/feature-chart`
- `features/feature-dom`
- `features/feature-trades`

Однако `composeApp` продолжает использовать старые классы из собственного пакета `nous_platform`:
- Старые ViewModel: `ChartViewModel`, `DomViewModel` (в пакете `nous_platform.ui.*`)
- Старые репозитории: `ChartRepositoryImpl`, `DomRepositoryImpl`, `BookTickerRepositoryImpl`
- Старые API клиенты: `BinanceCandlesApi`, `BinanceDomApi`, `BinanceBookTickerApi`, `BybitApi`
- Старый use case: `GetChartByTickerUseCase`

## Анализ различий API

### ChartViewModel

| Аспект | Старый (composeApp) | Новый (feature-chart) |
|--------|---------------------|----------------------|
| Пакет | `nous_platform.ui.chart` | `nous.feature.chart.ui` |
| Конструктор | `GetChartByTickerUseCase` | `ChartRepository`, `SymbolInfoAdapter` |
| `chartState` | `StateFlow<ChartState>` ✅ | `StateFlow<ChartState>` ✅ |
| `loadChart(symbol, timeframe)` | ✅ | ✅ (дополнительно: `selectSymbol`, `selectTimeframe`) |
| `ChartState` | sealed interface в `nous_platform.ui.chart` | sealed interface в `nous.feature.chart.ui` |
| `ChartConfig` | data class в `nous_platform.ui.chart` | data class в `nous.feature.chart.ui` |
| `CandleStickChart` | composable в `nous_platform.ui.chart` | composable в `nous.feature.chart.ui.chart` |

⚠️ **ChartState**, **ChartConfig** и **CandleStickChart** — разные типы в разных пакетах, хотя API совместимы.

### DomViewModel

| Аспект | Старый (composeApp) | Новый (feature-dom) |
|--------|---------------------|----------------------|
| Пакет | `nous_platform.ui.dom` | `nous.feature.dom.ui` |
| Конструктор | `DomRepository`, `BookTickerRepository` | `DomRepository`, `SymbolInfoRepository?` |
| `subscribeToOrderBook(symbol)` | ✅ явный вызов | ❌ авто-подписка через `init` и `updateDomOptions` |
| `selectedPrice` | `StateFlow<Double?>` ✅ | `StateFlow<Double?>` ✅ |
| `orderQuantity` | `StateFlow<String>` ✅ | `StateFlow<String>` ✅ |
| `isTradingEnabled` | `StateFlow<Boolean>` ✅ | `StateFlow<Boolean>` ✅ |
| `executeCommand(command)` | ✅ | ✅ |
| `bestPrices` | `StateFlow<BestPrices?>` | заменено на `incrementalBestBid/Ask` |
| `orderBook` | `StateFlow<OrderBook?>` | заменено на `incrementalBids/Asks` (SnapshotStateMap) |

⚠️ **Новый DomViewModel** имеет принципиально другую архитектуру: данные DOM хранятся в `SnapshotStateMap` для инкрементальных обновлений, нет отдельного `subscribeToOrderBook`, подписка управляется через `DomOptions`.

### TradesViewModel

| Аспект | Старый (в composeApp нет) | Новый (feature-trades) |
|--------|--------------------------|----------------------|
| Пакет | — | `nous.feature.trades.ui` |
| Конструктор | — | `TradesRepository` |
| API | — | `trades`, `subscribeToTrades(symbol)` |

✅ TradesViewModel уже используется в main.kt и MainScreen.kt из нового модуля.

## План изменений

### Шаг 1: AppModule.kt — переписать DI

Удалить старые DI-регистрации и заменить на новые из feature-модулей.

**Что удаляем:**
- `BinanceCandlesApi`, `BinanceDomApi`, `BinanceBookTickerApi`, `BybitApi`
- `ChartRepositoryImpl` (старый), `DomRepositoryImpl` (старый), `BookTickerRepositoryImpl` (старый)
- `ChartRepository`, `DomRepository`, `BookTickerRepository` (интерфейсы из nous_platform.domain)
- `GetChartByTickerUseCase`, `GetChartByTickerUseCaseImpl`
- `ChartViewModel` (старый из nous_platform.ui.chart)
- `DomViewModel` (старый из nous_platform.ui.dom)
- Дубликат `TradesRepository` (строка 133, дублирует строку 124)

**Что добавляем:**
- `coreModule` из `com.aandios.nous.core.di` (даёт `NetworkManager` + `HttpClient`)
- `ProviderConfig` singleton
- `Provider` singleton через `BinanceProviderFactory`
- Адаптеры из Provider: `DomAdapter`, `BookTickerAdapter`, `ChartAdapter`, `TradesAdapter`, `SymbolInfoAdapter`
- Репозитории из `platform-core`: `DomRepositoryImpl` (из feature-dom), `ChartRepositoryImpl`, `BookTickerRepositoryImpl`, `TradesRepositoryImpl`, `SymbolInfoRepositoryImpl`
- `ChartViewModel` (новый из `nous.feature.chart.ui`)
- `DomViewModel` (новый из `nous.feature.dom.ui`)
- `TradesViewModel` (уже используется, оставляем)
- `TerminalStateViewModel` (оставляем)

### Шаг 2: main.kt — обновить импорты

- `ChartViewModel` → `com.aandios.nous.feature.chart.ui.ChartViewModel`
- `DomViewModel` → `com.aandios.nous.feature.dom.ui.DomViewModel`

### Шаг 3: MainScreen.kt — обновить импорты и использование

**Изменения импортов:**
- `ChartViewModel` → `com.aandios.nous.feature.chart.ui.ChartViewModel`
- `ChartState` → `com.aandios.nous.feature.chart.ui.ChartState`
- `ChartConfig` → `com.aandios.nous.feature.chart.ui.ChartConfig`
- `CandleStickChart` → `com.aandios.nous.feature.chart.ui.chart.CandleStickChart`
- `DomViewModel` → `com.aandios.nous.feature.dom.ui.DomViewModel`
- `ChartColors` → `com.aandios.nous.core.ui.theme.ChartColors`

**Изменения в DOM-секции:**
Новый `DomViewModel` не требует вызова `subscribeToOrderBook(symbol)` — подписка управляется автоматически через `DomOptions`. 
Методы `onCommandResult` и `createTradeOffCommand` заменены на `handleOrderIntent`.

В MainScreen нужно:
1. Убрать `subscribeToOrderBook` из `LaunchedEffect`
2. Убрать `bestPrices` (заменено на инкрементальные bestBid/bestAsk)
3. Убрать `onCommandResult` 
4. Использовать `incrementalBids`/`incrementalAsks` и `incrementalBestBid`/`incrementalBestAsk` для отображения
5. Использовать `domOptions` для управления настройками

### Шаг 4: Очистка старых файлов (опционально)

Можно удалить неиспользуемые файлы из `composeApp`:
- `composeApp/src/jvmMain/kotlin/com/aandios/nous_platform/ui/chart/` — все файлы (заменены feature-chart)
- `composeApp/src/jvmMain/kotlin/com/aandios/nous_platform/ui/dom/` — все файлы (заменены feature-dom)
- `composeApp/src/jvmMain/kotlin/com/aandios/nous_platform/domain/` — usecases, entities, commands, repositories (заменены platform-core)
- `composeApp/src/jvmMain/kotlin/com/aandios/nous_platform/data/` — API клиенты и repository impl (заменены)

## Ключевые риски

1. **DomViewModel** — самое большое изменение. Новый ViewModel иначе управляет подпиской, данными и выполнением команд. Нужно аккуратно переписать DOM-виджет в MainScreen.
2. **ChartState** — sealed interface определён в двух местах. После перехода нужно убедиться, что MainScreen использует правильный.
3. **Koin-конфликты** — feature-модули определяют `featureChartModule`, `featureDomModule`, `featureTradesModule`, каждый со своими `ProviderConfig`/`Provider`. В `AppModule` нужно объявить Provider ТОЛЬКО ОДИН раз, не используя готовые feature-module.
4. **Candle** — старый использует `com.aandios.nous_platform.domain.entities.Candle`, новый использует `com.aandios.nous.api.market.model.Candle`. Нужно убедиться, что типы совместимы.
