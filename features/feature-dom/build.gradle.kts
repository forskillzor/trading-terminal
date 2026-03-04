plugins {
    id("conventions.kmp-feature")  // ✅ Должен работать после заполнения convention
    alias(libs.plugins.compose.compiler)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            // Только специфичные для DOM
//            implementation(project(":api:api-market"))
//            implementation(project(":api:api-ui"))
        }
    }
}