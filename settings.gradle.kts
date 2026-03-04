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
//include(":api:api-market")
//include(":api:api-ui")
//include(":core:core-base")
//include(":core:core-domain")
include(":core:core-dependencies")
include(":features:feature-dom")
//include(":app")