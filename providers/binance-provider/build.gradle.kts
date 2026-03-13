plugins {
    id("conventions.kmp-library")
    alias(libs.plugins.kotlin.serialization)

}
//apply(libs.plugins.kotlin.multiplatform)


kotlin {
    sourceSets {
        commonMain.dependencies {
            compileOnly(project(":public-api:api-market"))
            compileOnly(project(":core:core-dependencies"))
        }
        jvmMain.dependencies {
            // JVM-specific dependencies
        }
    }
}
//dependencies {
////    compileOnly(project(":public-api:api-market"))
//    compileOnly("io.ktor:ktor-client-core:3.4.1")
//    compileOnly("io.ktor:ktor-client-websockets:3.4.1")
//    compileOnly("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")
//    compileOnly("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.1")
//}

//tasks.jar {
//    manifest {
//        attributes(
//            "Implementation-Title" to "Binance Provider",
//            "Implementation-Version" to "1.0.0"
//        )
//    }
//
//    from(sourceSets.main.get().output)
//    from("src/main/resources") {
//        include("META-INF/services/**")
//    }
//}