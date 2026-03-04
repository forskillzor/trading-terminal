%%{init: {'theme': 'dark'}}%%

graph TB
%% Стили
classDef closed fill:#1E3A5F,stroke:#0288D1,stroke-width:2px,color:#fff
classDef open fill:#FBC02D,stroke:#F57F17,stroke-width:2px,color:#000
classDef plugin fill:#2E7D32,stroke:#1B5E20,stroke-width:2px,color:#fff
classDef jar fill:#BF360C,stroke:#E65100,stroke-width:2px,color:#fff
classDef external fill:#4A4A4A,stroke:#616161,stroke-width:2px,color:#fff
classDef note fill:#2A2A2A,stroke:#9E9E9E,color:#fff,stroke-dasharray: 5 5
classDef interface fill:#FFB74D,stroke:#F57C00,stroke-width:2px,color:#000
classDef database fill:#5D4037,stroke:#3E2723,stroke-width:2px,color:#fff

    %% ============ ЛЕГЕНДА ============
    subgraph Legend ["Условные обозначения"]
        direction LR
        L1[Закрытый код]:::closed
        L2[Открытый API]:::open
        L3[Плагины]:::plugin
        L4[JAR файлы]:::jar
        L5[Интерфейсы]:::interface
        L6[(База данных)]:::database
    end
    
    %% ============ ПЛАТФОРМА (ЗАКРЫТЫЙ КОД) ============
    subgraph Platform ["ПЛАТФОРМА - ВАШ IP (ЗАКРЫТЫЙ КОД)"]
        
        %% Приложение
        App["📱 app<br/>Точка входа<br/>main.kt, DI"]:::closed
        
        %% Фичи
        subgraph Features ["Фичи платформы"]
            direction TB
            ChartFeature["📈 feature-chart<br/>График<br/>- ChartWidget.kt<br/>- CandleStickChart.kt<br/>- ChartViewModel.kt"]:::closed
            DomFeature["📊 feature-dom<br/>Стакан (ваша реализация)<br/>- DomWidget.kt<br/>- DomSectionNinja.kt<br/>- DomViewModel.kt"]:::closed
            TradesFeature["💱 feature-trades<br/>Сделки<br/>- TradesWidget.kt<br/>- TradeRow.kt<br/>- TradesViewModel.kt"]:::closed
            TerminalFeature["🖥️ feature-terminal<br/>Лэйаут<br/>- TerminalLayout.kt<br/>- Panels"]:::closed
            PortfolioFeature["💰 feature-portfolio<br/>Портфель<br/>- PortfolioPanel.kt<br/>- PositionsList.kt"]:::closed
            IndicatorsFeature["📉 feature-indicators<br/>Индикаторы (ваши)"]:::closed
            StrategiesFeature["⚙️ feature-strategies<br/>Стратегии (ваши)"]:::closed
        end
        
        %% Ядро
        subgraph Core ["Ядро платформы"]
            direction TB
            CoreBase["core-base<br/>- utils/<br/>- extensions/<br/>- math/"]:::closed
            CoreDomain["core-domain<br/>- entities/<br/>- repositories/<br/>- usecases/"]:::closed
            CoreNetwork["core-network<br/>- http/<br/>- websocket/<br/>- client/"]:::closed
            CoreDI["core-di<br/>- CoreModule.kt<br/>- DI.kt"]:::closed
            CoreTheme["core-theme<br/>- Colors.kt<br/>- Typography.kt<br/>- Theme.kt"]:::closed
        end
        
        %% Плагинная система
        subgraph PluginSystem ["Плагинная система"]
            PluginLoader["plugin-loader<br/>- Сканирование JAR<br/>- Управление жизненным циклом"]:::closed
            ClassLoader["classloader/<br/>- Изолированные ClassLoader'ы<br/>- SandboxClassLoader.kt"]:::closed
            Security["security/<br/>- SignatureVerifier.kt<br/>- PermissionManager.kt<br/>- PluginSandbox.kt"]:::closed
            Registry["registry/<br/>- PluginRegistry.kt<br/>- PluginManifest.kt"]:::closed
        end
        
        %% База данных
        DB[(("Внутренняя БД<br/>SQLite"))]:::database
    end

    %% ============ ОТКРЫТЫЙ API ============
    subgraph OpenAPI ["ОТКРЫТЫЙ МОДУЛЬ ДЛЯ СООБЩЕСТВА"]
        API["📚 api"]:::open
        
        subgraph APIPackages ["Пакеты API"]
            MarketAPI["market/<br/>- MarketDataProvider<br/>- MarketDataSubscriber<br/>- models/*"]:::interface
            TradingAPI["trading/<br/>- TradingProvider<br/>- OrderManager<br/>- models/*"]:::interface
            AccountAPI["account/<br/>- AccountProvider<br/>- Balance, Account"]:::interface
            IndicatorsAPI["indicators/<br/>- Indicator<br/>- IndicatorContext<br/>- Parameter"]:::interface
            StrategiesAPI["strategies/<br/>- Strategy<br/>- StrategyContext<br/>- Signal"]:::interface
            RobotsAPI["robots/<br/>- TradingRobot<br/>- RobotConfig"]:::interface
            UIAPI["ui/<br/>- Widget<br/>- ChartWidget<br/>- OrderBookWidget"]:::interface
            ProviderAPI["provider/<br/>- ProviderFactory<br/>- ProviderConfig<br/>- ServiceDiscovery"]:::interface
        end
    end

    %% ============ ПЛАГИНЫ СООБЩЕСТВА ============
    subgraph CommunityPlugins ["ПЛАГИНЫ СООБЩЕСТВА (ОТКРЫТЫЙ КОД)"]
        direction TB
        
        subgraph ExchangePlugins ["Биржевые провайдеры"]
            BinancePlugin["provider-binance<br/>BinanceMarketProvider<br/>BinanceTradingProvider<br/>BinanceProviderFactory"]:::plugin
            BybitPlugin["provider-bybit<br/>BybitMarketProvider<br/>BybitProviderFactory"]:::plugin
            NewExchangePlugin["... другие биржи"]:::plugin
        end
        
        subgraph IndicatorPlugins ["Индикаторы"]
            RSIPlugin["rsi-indicator<br/>RSIIndicator"]:::plugin
            MACDPlugin["macd-indicator<br/>MACDIndicator"]:::plugin
            CustomIndicatorPlugin["custom-indicator<br/>..."]:::plugin
        end
        
        subgraph StrategyPlugins ["Стратегии"]
            StrategyAPlugin["ma-crossover<br/>MAStrategy"]:::plugin
            StrategyBPlugin["grid-trading<br/>GridStrategy"]:::plugin
            CustomStrategyPlugin["custom-strategy<br/>..."]:::plugin
        end
        
        subgraph RobotPlugins ["Торговые роботы"]
            RobotAPlugin["scalping-bot<br/>ScalpingRobot"]:::plugin
            RobotBPlugin["arbitrage-bot<br/>ArbitrageRobot"]:::plugin
            CustomRobotPlugin["trading-robot<br/>..."]:::plugin
        end
        
        subgraph UIPlugins ["UI компоненты"]
            CustomChartPlugin["custom-chart<br/>PieChartWidget"]:::plugin
            CustomDomPlugin["custom-dom<br/>HeatmapOrderBook"]:::plugin
            CustomWidgetPlugin["custom-widget<br/>..."]:::plugin
        end
    end

    %% ============ JAR ФАЙЛЫ ============
    JARFolder["📁 /plugins/ папка<br/>Скомпилированные JAR файлы"]:::jar

    %% ============ СВЯЗИ ВНУТРИ ПЛАТФОРМЫ ============
    App --> Features
    Features --> CoreDomain
    Features --> CoreBase
    Features --> CoreTheme
    
    CoreDomain --> CoreBase
    CoreDomain --> CoreNetwork
    CoreDomain --> DB
    
    App --> PluginSystem
    Core --> PluginSystem
    
    PluginSystem --> PluginLoader
    PluginLoader --> ClassLoader
    PluginLoader --> Security
    PluginLoader --> Registry
    
    %% ============ СВЯЗЬ ПЛАТФОРМЫ С API ============
    CoreDomain -.->|"использует интерфейсы"| API
    Features -.->|"использует интерфейсы"| API
    App -.->|"использует интерфейсы"| API
    
    %% ============ СВЯЗИ ПЛАГИНОВ С API ============
    BinancePlugin ==>|"реализует"| MarketAPI
    BinancePlugin ==>|"реализует"| TradingAPI
    BinancePlugin ==>|"реализует"| AccountAPI
    BinancePlugin ==>|"реализует"| ProviderAPI
    
    BybitPlugin ==>|"реализует"| MarketAPI
    BybitPlugin ==>|"реализует"| ProviderAPI
    
    RSIPlugin ==>|"реализует"| IndicatorsAPI
    MACDPlugin ==>|"реализует"| IndicatorsAPI
    
    StrategyAPlugin ==>|"реализует"| StrategiesAPI
    StrategyBPlugin ==>|"реализует"| StrategiesAPI
    
    RobotAPlugin ==>|"реализует"| RobotsAPI
    RobotBPlugin ==>|"реализует"| RobotsAPI
    
    CustomChartPlugin ==>|"реализует"| UIAPI
    CustomDomPlugin ==>|"реализует"| UIAPI

    %% ============ ПРОЦЕСС ЗАГРУЗКИ ПЛАГИНОВ ============
    BinancePlugin -.->|"компилируется в"| JARFolder
    RSIPlugin -.->|"компилируется в"| JARFolder
    StrategyAPlugin -.->|"компилируется в"| JARFolder
    RobotAPlugin -.->|"компилируется в"| JARFolder
    CustomChartPlugin -.->|"компилируется в"| JARFolder
    
    PluginLoader ==>|"1. сканирует папку"| JARFolder
    PluginLoader ==>|"2. проверяет подписи"| JARFolder
    PluginLoader ==>|"3. загружает JAR"| JARFolder
    PluginLoader ==>|"4. создает изолированные ClassLoader'ы"| JARFolder
    PluginLoader ==>|"5. регистрирует плагины"| JARFolder

    %% ============ ИСПОЛЬЗОВАНИЕ ПЛАГИНОВ ============
    ChartFeature -->|"отображает кастомные"| CustomChartPlugin
    DomFeature -->|"отображает кастомные"| CustomDomPlugin
    IndicatorsFeature -->|"использует"| RSIPlugin
    IndicatorsFeature -->|"использует"| MACDPlugin
    StrategiesFeature -->|"использует"| StrategyAPlugin
    StrategiesFeature -->|"использует"| StrategyBPlugin
    TerminalFeature -->|"подключает"| CustomWidgetPlugin
    App -->|"загружает провайдеры"| BinancePlugin
    App -->|"загружает провайдеры"| BybitPlugin

    %% ============ КЛЮЧЕВЫЕ КОМПОНЕНТЫ ============
    %% ИСПРАВЛЕНО: Убраны реальные переносы строк, заменены на <br/>
    KeyComponents["🔑 КЛЮЧЕВЫЕ КОМПОНЕНТЫ:<br/>📦 api - Единственный открытый модуль<br/>🔒 Всё остальное - закрыто (ваш IP)<br/>🔧 Плагины подключаются через:<br/>- ServiceLoader (META-INF/services)<br/>- Динамическая загрузка JAR<br/>- Изолированные ClassLoader'ы<br/>- Цифровые подписи<br/>🎯 Типы плагинов:<br/>- Провайдеры бирж<br/>- Индикаторы<br/>- Стратегии<br/>- Торговые роботы<br/>- UI компоненты (графики, стакан)<br/>🔒 Безопасность:<br/>- Песочница (Sandbox)<br/>- Ограничение доступа<br/>- Таймауты выполнения<br/>- Проверка подписей"]:::note
    
    KeyComponents -.- App

    %% ============ ПРИМЕЧАНИЯ ============
    NoteAPI["📌 АПИ ДЛЯ СООБЩЕСТВА<br/>- Только интерфейсы<br/>- Стабильная версия<br/>- Полная документация<br/>- Семантическое версионирование"]:::note
    NoteAPI -.- API
    
    NoteLoader["🔧 ЗАГРУЗЧИК ПЛАГИНОВ<br/>- Изолированные ClassLoader'ы<br/>- Песочница безопасности<br/>- Проверка подписей JAR<br/>- Ограничение времени выполнения<br/>- Управление зависимостями"]:::note
    NoteLoader -.- PluginLoader
    
    NoteJAR["📦 ДИНАМИЧЕСКАЯ ЗАГРУЗКА<br/>- Папка /plugins/<br/>- Горячая замена (hot-swap)<br/>- Без перекомпиляции<br/>- Версионирование плагинов"]:::note
    NoteJAR -.- JARFolder