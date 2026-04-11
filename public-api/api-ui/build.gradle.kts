plugins {
    id("conventions.kmp-library")
//    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
//            api(libs.kotlinx.coroutines.core)
//            api(libs.kotlinx.serialization.json)
//            api(libs.kotlinx.datetime)
        }
    }
}
