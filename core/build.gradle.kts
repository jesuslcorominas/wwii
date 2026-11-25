plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidKmpLibrary)
    alias(libs.plugins.kover)
}

// Game engine: pure Kotlin. Only commonMain/commonTest; no Compose, AndroidX,
// Firebase or platform APIs.
kotlin {
    jvmToolchain(17)

    jvm()

    android {
        namespace = "com.jesuslcorominas.wwii.core"
        compileSdk =
            libs.versions.android.compileSdk
                .get()
                .toInt()
        minSdk =
            libs.versions.android.minSdk
                .get()
                .toInt()
    }

    iosArm64()
    iosSimulatorArm64()

    @OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class)
    wasmJs {
        browser()
    }

    sourceSets {
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}
