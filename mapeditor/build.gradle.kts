plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

// Internal map digitizing tool (desktop only, not distributed). Depends on core's map model and
// JSON format; never on :ui.
kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation(projects.core)
    implementation(compose.desktop.currentOs)
    implementation(libs.compose.material3)
    implementation(libs.kotlinx.serialization.json)

    testImplementation(kotlin("test"))
}

compose.desktop {
    application {
        mainClass = "com.jesuslcorominas.wwii.mapeditor.MainKt"
    }
}
