# WWII (nombre provisional)

Juego de estrategia por turnos sobre la Segunda Guerra Mundial en Europa, para iOS, Android, web y escritorio.
Kotlin Multiplatform + Compose Multiplatform.

## Módulos

- `core`: motor del juego en Kotlin puro (solo `commonMain`), sin Compose ni APIs de plataforma.
- `ui`: pantallas en Compose Multiplatform.
- `androidApp`, `iosApp`, `desktopApp`, `webApp`: lanzadores de cada plataforma.

## Comandos

Requisitos: JDK 17, Android SDK (plataforma 37), Xcode y Chrome (tests Wasm).

```bash
./gradlew build                                 # build completo
./gradlew ktlintCheck                           # lint
./gradlew :core:allTests                        # tests del motor (JVM, iOS simulador, Wasm)
./gradlew koverXmlReport                        # cobertura de core
./gradlew :androidApp:installDebug              # Android
./gradlew :desktopApp:run                       # escritorio
./gradlew :webApp:wasmJsBrowserDevelopmentRun   # web en http://localhost:8080
```

iOS: abre `iosApp/iosApp.xcodeproj` en Xcode y ejecuta el esquema `iosApp` en un simulador.
