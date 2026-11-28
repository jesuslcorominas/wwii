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

// `./gradlew :mapeditor:run --args="data/maps/europe-1939.json"` resolves the path from the repo root,
// as the docs show, and not from this module's directory.
tasks.withType<JavaExec>().configureEach {
    workingDir = rootProject.projectDir
}
