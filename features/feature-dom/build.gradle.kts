plugins {
    id("conventions.kmp-feature")  // Теперь этот плагин применяет всё необходимое
    alias(libs.plugins.kotlin.serialization)  // только дополнительные плагины
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            // Зависимости уже добавлены в конвеншн-плагине,
            // но можно добавить специфичные для feature-dom:
            implementation(project(":platform-core"))
            implementation(project(":public-api:api-market"))
            implementation(project(":providers:binance-provider"))
            implementation(project(":composeApp"))

            implementation(libs.koin.core)
            implementation(libs.koin.compose)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.compose.material3)
        }
        commonTest.dependencies {
            // Kotlin Test (includes common and platform-specific implementations)
            implementation(libs.kotlin.test)
            
            // JUnit 5 for JVM
            implementation(libs.junit.jupiter)
            
            // Coroutines test
            implementation(libs.kotlinx.coroutines.test)
            
            // Ktor mock for HTTP client testing
            implementation(libs.ktor.client.mock)
        }
    }
}
