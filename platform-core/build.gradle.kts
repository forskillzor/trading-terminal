plugins {
    id("conventions.kmp-library")
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(project(":core:core-dependencies"))
            implementation(project(":public-api:api-market"))

            // Kotlin
            api(libs.kotlinx.coroutines.core)
            api(libs.kotlinx.serialization.json)
            api(libs.kotlinx.datetime)
            val ktorVersion = "3.4.1"
            val serializationVersion = "1.7.1"
            api("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")
            api("org.jetbrains.kotlinx:kotlinx-serialization-json:$serializationVersion")
            api("io.ktor:ktor-serialization-kotlinx-json:${ktorVersion}")
            api("io.insert-koin:koin-core:3.5.6")
            api("io.insert-koin:koin-compose:1.1.5")
            api("io.ktor:ktor-client-core:$ktorVersion")
            api("io.ktor:ktor-client-cio:$ktorVersion")
            api("io.ktor:ktor-client-websockets:$ktorVersion")
            api("io.ktor:ktor-client-content-negotiation:$ktorVersion")

            // Koin (для DI)
            api(libs.koin.core)
        }

        jvmMain.dependencies {
            // JVM-specific dependencies
        }
    }
}