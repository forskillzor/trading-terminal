plugins {
    id("conventions.kmp-library")
    id("conventions.kmp-application")
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(project(":core:core-dependencies"))
            implementation(project(":public-api:api-market"))
            implementation(project(":providers:binance-provider"))

            // Kotlin
            api(libs.kotlinx.coroutines.core)
            api(libs.kotlinx.serialization.json)
            api(libs.kotlinx.datetime)
            val ktorVersion = "3.4.1"
            val serializationVersion = "1.7.1"
            api("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")
            api("org.jetbrains.kotlinx:kotlinx-serialization-json:$serializationVersion")
            api("io.insert-koin:koin-core:3.5.6")
            api("io.insert-koin:koin-compose:1.1.5")
            api("io.ktor:ktor-serialization-kotlinx-json:${ktorVersion}")
            api("io.ktor:ktor-client-core:$ktorVersion")
            api("io.ktor:ktor-client-cio:$ktorVersion")
            api("io.ktor:ktor-client-websockets:$ktorVersion")
            api("io.ktor:ktor-client-content-negotiation:$ktorVersion")

            // Compose UI для темы
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(compose.components.resources)

            // Koin (для DI)
            api(libs.koin.core)
        }

        jvmMain {
            resources.srcDir("src/jvmMain/resources")
        }
        jvmMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(compose.components.uiToolingPreview)
        }
    }
}