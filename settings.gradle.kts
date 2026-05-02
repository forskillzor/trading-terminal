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
include(":features:feature-dom")
include(":features:feature-chart")
include(":features:feature-trades")
include("public-api:api-market")
include("public-api:api-trading")
include("public-api:api-ui")
include(":platform-core")
include("providers")
include("providers:binance-provider")