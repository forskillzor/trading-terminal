# План рефакторинга: Перенос Trades фичи в feature-trades

## Цель
Перенести код Trades (трейды/сделки) из монолита `composeApp` в отдельный модуль `features:feature-trades`, по аналогии с уже перенесёнными `feature-dom` и `feature-chart`. Создать `TradesWindow` с отдельной точкой входа.

## Текущая архитектура

### Старый код в composeApp (будет удалён)
- `composeApp/.../ui/trades/TradesViewModel.kt` — ViewModel для трейдов
- `composeApp/.../ui/trades/TradesWidget.kt` — UI компонент
- `composeApp/.../data/api/binance/BinanceTradesApi.kt` — Binance клиент
- `composeApp/.../data/repository/TradesRepositoryImpl.kt` — имплементация репозитория
- `composeApp/.../domain/repository/TradesRepository.kt` — интерфейс репозитория
- `composeApp/.../data/api/binance/models/Trade.kt`, `TradeSide.kt`, `BinanceAggTrade.kt` — дублирующие модели
- `composeApp/.../di/AppModule.kt` — регистрирует старые зависимости

### Новая инфраструктура (уже существует)
- `public-api/api-market/.../model/trades/Trade.kt` — @Serializable data class Trade
- `public-api/api-market/.../model/trading/TradeSide.kt` — enum TradeSide { BUY, SELL }
- `public-api/api-market/.../adapters/TradesAdapter.kt` — interface TradesAdapter
- `platform-core/.../domain/repository/TradesRepository.kt` — interface TradesRepository
- `platform-core/.../data/repository/TradesRepositoryImpl.kt` — impl, использует TradesAdapter
- `providers/binance-provider/.../adapter/BinanceTradesAdapter.kt` — impl TradesAdapter
- `providers/binance-provider/.../model/BinanceAggTrade.kt` — модель с `toTrade()`

### Reference модули
- `features/feature-chart` — ChartViewModel, ChartWindow, FeatureChartModule, build.gradle.kts
- `features/feature-dom` — DomViewModel, DomWindow, FeatureDomModule, build.gradle.kts

## Пошаговый план

### Шаг 1: Создать `features/feature-trades/build.gradle.kts`
```kotlin
plugins {
    id("conventions.kmp-feature")
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(project(":platform-core"))
            implementation(project(":public-api:api-market"))
            implementation(project(":providers:binance-provider"))

            implementation(libs.koin.core)
            implementation(libs.koin.compose)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.compose.material3)
        }

        jvmMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(libs.kotlinx.coroutines.swing)
        }

        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}

compose.desktop {
    application {
        mainClass = "com.aandios.nous.feature.trades.ui.TradesWindowKt"
    }
}
```

### Шаг 2: Создать `FeatureTradesModule.kt`
Путь: `features/feature-trades/src/commonMain/kotlin/com/aandios/nous/feature/trades/di/FeatureTradesModule.kt`

По аналогии с `FeatureChartModule`:
- `initKoinForPreview()` — останавливает старый контекст + запускает `coreModule` + `featureTradesModule`
- `featureTradesModule` — регистрирует:
  1. `ProviderConfig` (apiKey=null, testnet=false)
  2. `Provider` через `BinanceProviderFactory().createProvider(config, networkManager)`
  3. `TradesAdapter` из `get<Provider>().trades`
  4. `TradesRepository` -> `TradesRepositoryImpl(tradesAdapter)`
  5. `TradesViewModel` как `factory { TradesViewModel(tradesRepository = get()) }`

### Шаг 3: Создать `TradesViewModel.kt`
Путь: `features/feature-trades/src/commonMain/kotlin/com/aandios/nous/feature/trades/ui/TradesViewModel.kt`

Адаптировать старый TradesViewModel:
- Импортировать `com.aandios.nous.core.domain.repository.TradesRepository` (вместо composeApp версии)
- Импортировать `com.aandios.nous.api.market.model.trades.Trade` (новая модель)
- Сохранить ту же логику:
  - `subscribeToTrades(symbol)` — подписка через репозиторий
  - `_trades: MutableStateFlow<List<Trade>>` — стейт с макс 100 трейдами
  - `formatTime(timestamp: Long): String` — форматирование времени
  - `formatPrice(price: Double): String` — форматирование цены
  - `formatQuantity(quantity: Double): String` — форматирование объёма
  - `clear()` — очистка корутин

### Шаг 4: Создать `TradesWidget.kt`
Путь: `features/feature-trades/src/commonMain/kotlin/com/aandios/nous/feature/trades/ui/TradesWidget.kt`

Адаптировать старый TradesWidget:
- Заменить импорты на `com.aandios.nous.api.market.model.trades.Trade` и `com.aandios.nous.api.market.model.trading.TradeSide`
- Сохранить те же компоненты: `TradesWidget`, `TradesHeader`, `TradesList`, `TradeRow`
- Та же логика отображения (зелёный для BUY, красный для SELL, monospace шрифты)

### Шаг 5: Создать `TradesWindow.kt`
Путь: `features/feature-trades/src/commonMain/kotlin/com/aandios/nous/feature/trades/ui/TradesWindow.kt`

По аналогии с `ChartWindow.kt`:
```kotlin
package com.aandios.nous.feature.trades.ui

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.aandios.nous.core.ui.theme.TradingTerminalTheme
import com.aandios.nous.feature.trades.di.initKoinForPreview
import org.koin.compose.KoinContext
import org.koin.compose.koinInject
import org.koin.core.context.stopKoin

@Composable
fun TradesWindowContent() {
    val viewModel: TradesViewModel = koinInject()
    val trades by viewModel.trades.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.subscribeToTrades("BTCUSDT")
    }

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        TradesWidget(
            viewModel = viewModel,
            modifier = Modifier.fillMaxSize()
        )
    }
}

fun main() = application {
    stopKoin()
    initKoinForPreview()

    Window(
        onCloseRequest = ::exitApplication,
        title = "Nous Platform • Trades Preview",
        state = rememberWindowState(width = 400.dp, height = 600.dp)
    ) {
        KoinContext {
            TradingTerminalTheme {
                TradesWindowContent()
            }
        }
    }
}
```

### Шаг 6: Зарегистрировать модуль в `settings.gradle.kts`
Добавить строку:
```kotlin
include(":features:feature-trades")
```

### Шаг 7: Обновить `composeApp/AppModule.kt`
- Заменить старую регистрацию `TradesRepository`/`TradesRepositoryImpl`:
  - Вместо `BinanceTradesApi` + старый `TradesRepositoryImpl` — использовать `TradesRepositoryImpl` из `platform-core`
  - Нужно будет добавить зависимость composeApp на `:platform-core` и `:public-api:api-market`
- Заменить старый `TradesViewModel` — импортировать из `com.aandios.nous.feature.trades.ui.TradesViewModel`

Важно: Для composeApp нужна DI цепочка:
```kotlin
// 1. ProviderConfig
single<ProviderConfig> { ProviderConfig(...) }

// 2. Provider (Binance)
single<Provider> { BinanceProviderFactory().createProvider(get(), get()) }

// 3. Адаптер
single<TradesAdapter> { get<Provider>().trades ?: error(...) }

// 4. Репозиторий (из platform-core)
single<TradesRepository> { TradesRepositoryImpl(tradesAdapter = get()) }

// 5. ViewModel (из feature-trades)
factory { TradesViewModel(tradesRepository = get()) }
```

### Шаг 8: Обновить `composeApp/main.kt` и `MainScreen.kt`
- `main.kt`: изменить импорт `TradesViewModel` на `com.aandios.nous.feature.trades.ui.TradesViewModel`
- `MainScreen.kt`: изменить импорты `TradesViewModel` и `TradesWidget` на версии из feature-trades

### Шаг 9: Удалить старый код из composeApp
Удалить файлы:
- `composeApp/src/jvmMain/kotlin/.../ui/trades/TradesViewModel.kt`
- `composeApp/src/jvmMain/kotlin/.../ui/trades/TradesWidget.kt`
- `composeApp/src/jvmMain/kotlin/.../data/api/binance/BinanceTradesApi.kt`
- `composeApp/src/jvmMain/kotlin/.../data/repository/TradesRepositoryImpl.kt`
- `composeApp/src/jvmMain/kotlin/.../domain/repository/TradesRepository.kt`
- `composeApp/src/jvmMain/kotlin/.../data/api/binance/models/Trade.kt` (если отдельный файл)
- `composeApp/src/jvmMain/kotlin/.../data/api/binance/models/BinanceTradeModels.kt` (если содержит Trade, TradeSide, BinanceAggTrade)

### Шаг 10: Обновить зависимости composeApp
Проверить/добавить в `composeApp/build.gradle.kts`:
```kotlin
commonMain.dependencies {
    implementation(project(":platform-core"))
    implementation(project(":public-api:api-market"))
    implementation(project(":features:feature-trades"))
    // ... остальное
}
```

## Потенциальные риски

1. **Не найден `settings.gradle.kts`** — без него нельзя зарегистрировать новый модуль. Нужно его найти или создать.
2. **Конфликт моделей** — старые модели `Trade`/`TradeSide` в composeApp нужно полностью удалить, иначе будут конфликты имён
3. **Зависимости composeApp** — нужно убедиться, что composeApp имеет доступ к platform-core, public-api:api-market, providers:binance-provider
4. **`BinanceBookTickerApi`** — в AppModule есть `BinanceBookTickerApi` и `BookTickerRepository`, они НЕ связаны с Trades, их трогать не нужно
5. **`TerminalLayout.kt`** — импортирует `TradeSide` из composeApp (строка 12), нужно исправить импорт на `com.aandios.nous.api.market.model.trading.TradeSide`
