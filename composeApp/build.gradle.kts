import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeHotReload)
    alias(libs.plugins.compose.compiler)
}

kotlin {
    jvm()

    sourceSets {
        commonMain.dependencies {
            // Koin для десктопа
            implementation("io.insert-koin:koin-core:3.5.0")
            implementation("io.insert-koin:koin-compose:1.1.0")

            // Ktor для работы с биржами
            implementation("io.ktor:ktor-client-core:2.3.5")
            implementation("io.ktor:ktor-client-cio:2.3.5")
            implementation("io.ktor:ktor-client-websockets:2.3.5")
            implementation("io.ktor:ktor-client-content-negotiation:2.3.5")
            implementation("io.ktor:ktor-serialization-kotlinx-json:2.3.5")

            // kotlinx datetime
            implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.4.1")

            // Для удобства работы с временем
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")

            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
        jvmMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(libs.kotlinx.coroutinesSwing)
            implementation(libs.androidx.ui.geometry.desktop)
        }
    }
}


compose.desktop {
    application {
        mainClass = "com.aandios.tradingterminal.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "com.aandios.tradingterminal"
            packageVersion = "1.0.0"
            buildTypes.release.proguard {
                // 1. Отключаем оптимизации и обфускацию
                obfuscate.set(false)
                optimize.set(false)

                // 2. Добавляем кастомные правила ИГНОРИРОВАНИЯ всех warnings
                configurationFiles.from(
                    // Создаём временный файл с правилами
                    providers.provider {
                        val proguardFile = file("$buildDir/tmp/proguard-rules.pro")
                        proguardFile.parentFile.mkdirs()

                        proguardFile.writeText("""
                            # Отключаем всё, что может сломать сборку
                            -dontobfuscate
                            -dontoptimize
                            -dontshrink
                            -dontpreverify
                            -ignorewarnings
                            
                            # Игнорируем ВСЕ warnings (ProGuard требовательный)
                            -dontwarn **
                            
                            # Сохраняем наш код
                            -keep class com.aandios.tradingterminal.** { *; }
                            -keep class com.aandios.tradingterminal.**$* { *; }
                            
                            # Сохраняем Kotlin
                            -keep class kotlin.** { *; }
                            -keep class kotlinx.** { *; }
                            
                            # Сохраняем Compose
                            -keep class androidx.compose.** { *; }
                            -keep class androidx.ui.** { *; }
                            
                            # Сохраняем Ktor
                            -keep class io.ktor.** { *; }
                            
                            # Сохраняем Koin
                            -keep class org.koin.** { *; }
                            
                            # Сохраняем сериализацию
                            -keep class kotlinx.serialization.** { *; }
                            
                            # Сохраняем корутины
                            -keep class kotlinx.coroutines.** { *; }
                            
                            # Игнорируем конкретные ошибки из лога
                            -dontwarn androidx.compose.ui.util.ListUtilsKt
                            -dontwarn org.slf4j.**
                            -dontwarn org.slf4j.impl.**
                            -dontwarn kotlinx.coroutines.internal.LockFreeLinkedListHead
                            -dontwarn androidx.compose.ui.geometry.MutableRect
                            
                            # Разрешаем дубликаты ресурсов (те Note про MANIFEST.MF)
                            -ignorewarnings
                        """.trimIndent())

                        proguardFile
                    }
                )
            }
            windows {
                menu = true
                // iconFile.set(project.file("src/jvmMain/resources/icon.ico"))
            }

            macOS {
                // bundleID = "com.aandios.tradingterminal"
                // iconFile.set(project.file("src/jvmMain/resources/icon.icns"))
            }

            linux {
                shortcut = true
                // iconFile.set(project.file("src/jvmMain/resources/icon.png"))
            }
        }
    }
}
