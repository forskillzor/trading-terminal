plugins {
    id("conventions.kmp-library")
    alias(libs.plugins.kotlin.serialization)

}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(project(":public-api:api-market"))
            api(project(":core:core-dependencies"))
        }
        jvmMain.dependencies {
            implementation("io.ktor:ktor-client-cio:3.4.1")
        }
    }
}