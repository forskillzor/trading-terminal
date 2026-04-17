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