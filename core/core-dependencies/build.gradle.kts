plugins {
    id("conventions.kmp-library")  // ✅ Этот плагин должен существовать
    alias(libs.plugins.compose.multiplatform)  // Добавить эту строку
    alias(libs.plugins.compose.compiler)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            // Все общие зависимости
            api("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")
            api("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.1")
            api("io.insert-koin:koin-core:3.5.6")
            api("io.insert-koin:koin-compose:1.1.5")
            api("io.ktor:ktor-client-core:3.0.0")
            api(compose.runtime)
        }

        jvmMain.dependencies {
            api("io.ktor:ktor-client-cio:3.0.0")
            api("io.ktor:ktor-client-websockets:3.0.0")
            api(compose.desktop.currentOs)
        }
    }
}