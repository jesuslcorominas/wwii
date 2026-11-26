plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.androidKmpLibrary)
    alias(libs.plugins.kover)
}

// Game engine: pure Kotlin. Only commonMain/commonTest; no Compose, AndroidX,
// Firebase or platform APIs. kotlinx.serialization is the only external library.
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
        commonMain.dependencies {
            implementation(libs.kotlinx.serialization.json)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}

// Game data at the repo root (data/). Only the JVM tests read it, to validate the real files; the
// engine itself always receives the text.
val gameDataDir = rootProject.layout.projectDirectory.dir("data")

tasks.named<Test>("jvmTest") {
    inputs.dir(gameDataDir).withPropertyName("gameData")
    systemProperty("wwii.dataDir", gameDataDir.asFile.absolutePath)
}
