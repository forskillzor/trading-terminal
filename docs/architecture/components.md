@startuml
!include https://raw.githubusercontent.com/plantuml-stdlib/C4-PlantUML/master/C4_Container.puml

Person(developer, "Разработчик")
System(terminal, "Trading Terminal", "Kotlin/Compose")
System(backend, "Backend Aggregator", "Ktor/Redis")
System(binance, "Binance API", "WebSocket")

Rel(developer, terminal, "Использует")
Rel(terminal, backend, "REST/WebSocket")
Rel(backend, binance, "Подписка на данные")
@enduml