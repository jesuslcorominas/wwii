import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation(projects.ui)
    implementation(compose.desktop.currentOs)
}

compose.desktop {
    application {
        mainClass = "com.jesuslcorominas.wwii.desktop.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "wwii"
            packageVersion = providers.gradleProperty("app.versionName").get()
        }
    }
}
