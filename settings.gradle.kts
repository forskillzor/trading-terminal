rootProject.name = "Nous-Platform"

pluginManagement {
    includeBuild("build-logic")
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
}

include(":composeApp")
include(":core:core-dependencies")
include(":features:dom")
include(":features:chart")
include(":features:trades")
include(":features:localstorage")
include(":features:settings")
include("public-api:api-market")
include("public-api:api-trading")
include("public-api:api-ui")
include(":platform-core")
include("providers")
include("providers:binance-provider")