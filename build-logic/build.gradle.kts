plugins {
    `kotlin-dsl`
    `java-gradle-plugin`
}

repositories {
    mavenCentral()
    gradlePluginPortal()
}

dependencies {
    // Эти зависимости нужны для convention плагинов
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:2.1.20")
    implementation("org.jetbrains.compose:compose-gradle-plugin:1.7.0")
}

gradlePlugin {
    plugins {
        create("kmp-feature") {
            id = "conventions.kmp-feature"
            implementationClass = "conventions.KmpFeatureConvention"
        }
        create("kmp-library") {
            id = "conventions.kmp-library"
            implementationClass = "conventions.KmpLibraryConvention"
        }
        create("kmp-application") {
            id = "conventions.kmp-application"
            implementationClass = "conventions.KmpApplicationConvention"
        }
    }
}