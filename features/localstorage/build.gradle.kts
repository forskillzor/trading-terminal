plugins {
    id("conventions.kmp-feature")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(project(":platform-core"))
            implementation(libs.koin.core)
            implementation(libs.kotlinx.coroutines.core)
        }
        jvmMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation("org.xerial:sqlite-jdbc:3.49.1.0")
        }
    }
}
