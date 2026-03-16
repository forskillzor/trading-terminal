# Provider Plugin Guide

This document describes how to create a new exchange provider plugin for the Nous platform.

## Overview

Providers are plugins that implement market data and trading functionality for a specific exchange (e.g., Binance, Bybit). They are discovered at runtime via the Java `ServiceLoader` mechanism and can be loaded either from the classpath (built‑in) or from external JAR files placed in the `plugins/` directory.

The plugin system is built around two main interfaces:

- `ProviderFactory` – creates a `Provider` instance for a given configuration.
- `Provider` – exposes adapters for different market data streams and trading operations.

## Creating a New Provider

### 1. Project Structure

Create a new Gradle module (or a standalone JAR) with the following dependencies:

```kotlin
dependencies {
    implementation(project(":public-api:api-market"))
    // other dependencies (ktor, serialization, etc.)
}
```

### 2. Implement the Adapters

Choose which adapters your exchange supports. The available adapter types are defined in `AdapterType`:

- `TRADES` – real‑time trade stream
- `DOM` – order book (depth) stream
- `BOOK_TICKER` – best bid/ask updates
- `CHART` – historical and real‑time candles
- `TRADING` – order placement, cancellation, etc.

Each adapter is a separate interface extending `MarketAdapter`. Implement the required suspend functions and flows.

Example: `BinanceDomAdapter`, `BinanceBookTickerAdapter`.

### 3. Implement the Provider

Create a class that implements `Provider`:

```kotlin
class MyProvider(
    override val providerId: String,
    override val providerName: String,
    override val version: String,
    override val adapters: Map<AdapterType, MarketAdapter>
) : Provider {

    override suspend fun isAvailable(): Boolean = …

    override suspend fun getAvailableSymbols(): List<Symbol> = …

    // Optional lifecycle methods (added in API version 1.1)
    override suspend fun start() {
        // start websockets, timers, etc.
    }

    override suspend fun stop() {
        // clean up resources
    }
}
```

### 4. Implement the ProviderFactory

Create a factory that builds your provider with the required adapters:

```kotlin
class MyProviderFactory : ProviderFactory {

    override val providerId = "my-exchange"
    override val providerName = "My Exchange"
    override val version = "1.0.0"
    override val supportedAdapters = setOf(
        AdapterType.TRADES,
        AdapterType.DOM,
        AdapterType.BOOK_TICKER
    )

    override suspend fun validateConfig(config: ProviderConfig): ValidationResult {
        // validate API keys, URLs, etc.
        return if (config.apiKey.isNullOrEmpty())
            ValidationResult.Error("API key is required")
        else
            ValidationResult.Success
    }

    override suspend fun createProvider(
        config: ProviderConfig,
        networkManager: NetworkManager
    ): Provider {
        val client = networkManager.httpClient
        val adapters = mapOf(
            AdapterType.TRADES to MyTradesAdapter(client, config),
            AdapterType.DOM to MyDomAdapter(client, config),
            AdapterType.BOOK_TICKER to MyBookTickerAdapter(client, config)
        )
        return MyProvider(
            providerId = providerId,
            providerName = providerName,
            version = version,
            adapters = adapters
        )
    }
}
```

### 5. Service Registration

Create a file `src/main/resources/META‑INF/services/com.aandios.nous.api.market.ProviderFactory` containing the fully qualified class name of your factory:

```
com.example.myexchange.MyProviderFactory
```

This allows the `ServiceLoader` to discover your plugin automatically.

### 6. Configuration Validation

The `validateConfig` method is called before creating a provider. You can return a detailed error message with a map of invalid fields. If validation fails, the platform will not instantiate the provider.

### 7. Lifecycle Management

If your provider needs to establish persistent connections (WebSocket, timers), implement the `start()` and `stop()` methods. The platform will call `start()` after creating the provider and `stop()` before discarding it.

### 8. Error Handling

Use the dedicated exception hierarchy when throwing errors:

- `ProviderException` – general provider errors
- `AdapterException` – adapter‑specific errors
- `NetworkException` – network communication failures
- `ConfigValidationException` – configuration validation errors

This ensures consistent error reporting and logging.

## Loading Providers in the Core Application

The core platform uses `ProviderRegistry` (or `PluginLoader`) to discover all available providers.

```kotlin
val registry = ProviderRegistry()
val factories = registry.getAllFactories()
val binanceFactory = registry.getFactory("binance")

val config = ProviderConfig(apiKey = "…", secretKey = "…")
val provider = registry.getProvider("binance", config, httpClient)
```

Providers can be filtered by supported adapters:

```kotlin
val domFactories = registry.getFactoriesByAdapterType(AdapterType.DOM)
```

## Packaging and Deployment

- **Built‑in providers** – include the module in the Gradle build; the provider will be loaded from the classpath.
- **External JARs** – place the compiled JAR (with all dependencies shaded if needed) into the `plugins/` directory next to the application.

The platform also supports dynamic loading/unloading of JAR‑based plugins at runtime (see `PluginLoader`).

## Versioning and Compatibility

The public API follows semantic versioning. Provider modules should specify the `apiVersion` they target. Breaking changes in the API will be reflected in the major version number.

## Example

Refer to the existing `binance‑provider` module for a complete, production‑ready example.

## Troubleshooting

- **Provider not found** – ensure the service file is present in the JAR and the class name is correct.
- **Validation errors** – check the logs for `ValidationResult.Error` messages.
- **Adapter missing** – verify that the adapter is listed in `supportedAdapters` and added to the `adapters` map.

## Further Reading

- [Architecture Overview](../architecture/project-diagram.md)
- [API Reference](../api/rest-api.md) (TODO)
- [WebSocket API](../api/websocket-api.md) (TODO)