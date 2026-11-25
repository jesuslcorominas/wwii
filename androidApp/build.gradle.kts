plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeCompiler)
}

kotlin {
    jvmToolchain(17)
}

android {
    namespace = "com.jesuslcorominas.wwii.android"
    compileSdk =
        libs.versions.android.compileSdk
            .get()
            .toInt()

    defaultConfig {
        applicationId = "com.jesuslcorominas.wwii"
        minSdk =
            libs.versions.android.minSdk
                .get()
                .toInt()
        targetSdk =
            libs.versions.android.targetSdk
                .get()
                .toInt()
        versionCode = providers.gradleProperty("app.versionCode").get().toInt()
        versionName = providers.gradleProperty("app.versionName").get()
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(projects.ui)
    implementation(libs.androidx.activity.compose)
}
