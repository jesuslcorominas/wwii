plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidKmpLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

kotlin {
    jvmToolchain(17)

    jvm()

    android {
        namespace = "com.jesuslcorominas.wwii.ui"
        compileSdk =
            libs.versions.android.compileSdk
                .get()
                .toInt()
        minSdk =
            libs.versions.android.minSdk
                .get()
                .toInt()
    }

    // Framework consumed by iosApp through embedAndSignAppleFrameworkForXcode.
    listOf(iosArm64(), iosSimulatorArm64()).forEach { target ->
        target.binaries.framework {
            baseName = "WwiiUi"
            isStatic = true
        }
    }

    @OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class)
    wasmJs {
        browser()
    }

    sourceSets {
        commonMain.dependencies {
            implementation(projects.core)
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.ui)
            implementation(libs.compose.material3)
        }
    }
}

// ui has no Wasm UI tests yet, and the Compose guard fails `build` when a wasmJs library
// target declares no executable binary (CMP-4906). Re-enable it, together with
// binaries.executable(), when Compose UI tests for Wasm are added.
tasks.matching { it.name == "checkComposeUiTestConfigurationForWasmJs" }.configureEach {
    enabled = false
}
