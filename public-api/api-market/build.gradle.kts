plugins {
    id("conventions.kmp-library")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(libs.ktor.client.core)
        }
    }
}