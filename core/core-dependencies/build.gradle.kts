plugins {
    id("conventions.kmp-library")  // ✅ Этот плагин должен существовать
    alias(libs.plugins.compose.multiplatform)  // Добавить эту строку
    alias(libs.plugins.compose.compiler)
}

val ktorVersion = "3.4.1"
val serializationVersion = "1.7.1"
kotlin {
    sourceSets {
        commonMain.dependencies {
            // Все общие зависимости
            api("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")
            api("org.jetbrains.kotlinx:kotlinx-serialization-json:${serializationVersion}")
            api("io.insert-koin:koin-core:3.5.6")
            api("io.insert-koin:koin-compose:1.1.5")
            api("io.ktor:ktor-serialization-kotlinx-json:${ktorVersion}")
            api("io.ktor:ktor-client-core:${ktorVersion}")
            api("io.ktor:ktor-client-cio:${ktorVersion}")
            api("io.ktor:ktor-client-websockets:${ktorVersion}")
            api("io.ktor:ktor-client-content-negotiation:${ktorVersion}")
            api(compose.runtime)
        }

        jvmMain.dependencies {
            api(compose.desktop.currentOs)
        }
    }
}