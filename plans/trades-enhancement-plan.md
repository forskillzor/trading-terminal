# Plan: feature-trades Enhancement (Theme + Symbol Selector + Size Filter)

## Overview
Три доработки для панели `feature-trades`:
1. Применить тему/цвета как в `feature-dom` (MaterialTheme.colorScheme)
2. Добавить `SymbolSelector` dropdown с поиском/фильтрацией (аналог SymbolDropdown из feature-dom)
3. Добавить `SizeFilter` dropdown для фильтрации сделок по минимальному объёму (minQty из SymbolInfo)

---

## Files to Modify

### 1. [`features/feature-trades/src/commonMain/kotlin/com/aandios/nous/feature/trades/ui/TradesViewModel.kt`](../features/feature-trades/src/commonMain/kotlin/com/aandios/nous/feature/trades/ui/TradesViewModel.kt)

**Changes:**
- Inject `SymbolInfoRepository` (из `platform-core` — уже доступен в `AppModule`)
- Add `loadedSymbols: StateFlow<List<SymbolInfo>>` — загружается через `symbolInfoRepository.getAllSymbolsInfo()` при инициализации
- Add `currentSymbolInfo: StateFlow<SymbolInfo?>` — обновляется при смене символа через `symbolInfoRepository.getSymbolInfo(symbol)`
- Add `minTradeSize: StateFlow<Double?>` — берётся из `currentSymbolInfo.value?.minQty`
- Add `selectedSizeFilter: MutableStateFlow<SizeFilter>` — enum: `All`, `MinQty`, `MinQty * 10`, `MinQty * 100`, `Custom(Double)`
- Add `filteredTrades: StateFlow<List<Trade>>` — derived from `_state` + `selectedSizeFilter`, фильтрует по `trade.quantity >= threshold`
- Move `subscribeToTrades` to also refresh `currentSymbolInfo` and `minTradeSize`
- Remove `currentSymbol` tracking (now use symbol directly)

**State additions:**
```kotlin
enum class SizeFilter(val label: String) {
    All("All"),
    MinQty("≥ min"),
    MinQtyx10("≥ ×10"),
    MinQtyx100("≥ ×100"),
}
```

### 2. [`features/feature-trades/src/commonMain/kotlin/com/aandios/nous/feature/trades/ui/TradesWidget.kt`](../features/feature-trades/src/commonMain/kotlin/com/aandios/nous/feature/trades/ui/TradesWidget.kt)

**Changes:**
- Replace ALL hardcoded colors with `MaterialTheme.colorScheme`:
  - `BuyColor` → `MaterialTheme.colorScheme.primary` (зелёный buy)
  - `SellColor` → `MaterialTheme.colorScheme.secondary` (красный sell)
  - `HeaderBg` → `MaterialTheme.colorScheme.surfaceVariant`
  - `RowBgEven` → `Color.Transparent` (или surfaceVariant.copy(alpha=0.3f))
  - `RowBgOdd` → `Color.Transparent`
  - Gray text → `MaterialTheme.colorScheme.onSurfaceVariant`
- Add **header bar** at top:
  - `Surface(color = MaterialTheme.colorScheme.surfaceVariant, tonalElevation = 1.dp)`
  - Row with: [SymbolSelector] [SizeFilter] [Live indicator dot]
- Replace `HeaderRow()` with a styled header inside the Surface
- Pass `symbol` and `onSymbolChanged` callback
- Pass `sizeFilter` and `onSizeFilterChanged` callback
- Display filtered list instead of full list

### 3. [`features/feature-trades/src/commonMain/kotlin/com/aandios/nous/feature/trades/ui/TradesWindow.kt`](../features/feature-trades/src/commonMain/kotlin/com/aandios/nous/feature/trades/ui/TradesWindow.kt)

**Changes:**
- Update to pass new props through to `TradesWidget`
- No major changes — already uses `TradingTerminalTheme` via KoinContext

---

## New Files to Create

### 4. [`features/feature-trades/src/commonMain/kotlin/com/aandios/nous/feature/trades/ui/header/TradesSymbolDropdown.kt`](../features/feature-trades/src/commonMain/kotlin/com/aandios/nous/feature/trades/ui/header/TradesSymbolDropdown.kt)

**Purpose:** Lightweight symbol selector (не зависит от `feature-dom`).
- Uses `SymbolInfo` (из `api-market`) напрямую — не тянет `TradingSymbol` из feature-dom
- Pattern: порт `SymbolDropdown.kt` из `feature-dom`:
  - `TerminalDropdownWithLabel(label = "Sym")` — обёртка
  - `BasicTextField` для поиска (фильтр по `symbol`, `baseAsset`, `quoteAsset`)
  - `DropdownMenu` с отфильтрованным списком
  - Fallback на статический список (`BTCUSDT`, `ETHUSDT`, etc.) если `loadedSymbols` пуст

**Signature:**
```kotlin
@Composable
fun TradesSymbolDropdown(
    currentSymbol: String,
    availableSymbols: List<SymbolInfo>,
    onSymbolChanged: (String) -> Unit,
    modifier: Modifier = Modifier
)
```

### 5. [`features/feature-trades/src/commonMain/kotlin/com/aandios/nous/feature/trades/ui/header/SizeFilterDropdown.kt`](../features/feature-trades/src/commonMain/kotlin/com/aandios/nous/feature/trades/ui/header/SizeFilterDropdown.kt)

**Purpose:** Dropdown для фильтрации сделок по размеру.
- Использует `minQty` из `SymbolInfo` для генерации опций:
  - `All` — без фильтра
  - `≥ {minQty}` — минимальный объём
  - `≥ {minQty × 10}` 
  - `≥ {minQty × 100}`
- Если `minQty == null` (не загрузился), показывает только `All`
- Использует `TerminalDropdownWithLabel(label = "Size")`

**Signature:**
```kotlin
@Composable
fun SizeFilterDropdown(
    currentFilter: SizeFilter,
    minQty: Double?,
    onFilterChanged: (SizeFilter) -> Unit,
    modifier: Modifier = Modifier
)
```

---

## DI Changes

### 6. [`composeApp/src/jvmMain/kotlin/com/aandios/nous_platform/di/AppModule.kt`](../composeApp/src/jvmMain/kotlin/com/aandios/nous_platform/di/AppModule.kt)

**Change (line 117-120):**
```kotlin
factory {
    TradesViewModel(
        tradesRepository = get(),
        symbolInfoRepository = get()  // <-- ADDED
    )
}
```

### 7. [`features/feature-trades/src/commonMain/kotlin/com/aandios/nous/feature/trades/di/FeatureTradesModule.kt`](../features/feature-trades/src/commonMain/kotlin/com/aandios/nous/feature/trades/di/FeatureTradesModule.kt)

**Change (line 62-65):**
```kotlin
factory {
    TradesViewModel(
        tradesRepository = get(),
        symbolInfoRepository = get()  // <-- ADDED
    )
}
```

Also need to add `SymbolInfoRepository` binding in preview module:
```kotlin
// 4b. SymbolInfoRepository
single<SymbolInfoRepository> {
    SymbolInfoRepositoryImpl(symbolInfoAdapter = get())
}
```
And `SymbolInfoAdapter` binding:
```kotlin
single<SymbolInfoAdapter> {
    get<Provider>().symbolInfo ?: error("SymbolInfo adapter not available")
}
```

---

## Execution Order

| Step | File | What |
|------|------|------|
| 1 | `TradesViewModel.kt` | Add `SymbolInfoRepository`, `loadedSymbols`, `currentSymbolInfo`, size filter state |
| 2 | `TradesSymbolDropdown.kt` | Create new file — port of SymbolDropdown.kt |
| 3 | `SizeFilterDropdown.kt` | Create new file — filter dropdown |
| 4 | `TradesWidget.kt` | Apply theme colors, add header bar with dropdowns |
| 5 | `TradesWindow.kt` | Minor adjustments if needed |
| 6 | `FeatureTradesModule.kt` | Add SymbolInfoRepository + SymbolInfoAdapter bindings |
| 7 | `AppModule.kt` | Add `symbolInfoRepository = get()` to TradesViewModel factory |
