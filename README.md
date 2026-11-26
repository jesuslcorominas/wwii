# WWII (nombre provisional)

Juego de estrategia por turnos sobre la Segunda Guerra Mundial en Europa y el norte de África
(1939-1945), para Android, iOS, web y escritorio.

Se juega sobre un mapa hexagonal con hasta cuatro jugadores en tres bandos: el Eje, los Aliados
Occidentales y la URSS. Cada partida elige uno de dos reglamentos: **Clásico**, más sencillo, y
**Avanzado**, con más detalle. Habrá dos formas de jugar: la campaña completa de 18 turnos y
escenarios cortos de 4 a 6 turnos centrados en una región concreta.

> **Juego original.** Este proyecto está inspirado en las mecánicas de los wargames clásicos de
> tablero, pero el nombre, el mapa, los gráficos, los textos de las reglas y las tablas son propios.
> No usa marcas, tableros, fichas ni textos de ningún juego publicado.

## Estado

En desarrollo temprano. Hoy el repo contiene el esqueleto del proyecto: el motor (`core`) y la
interfaz (`ui`) arrancan en las cuatro plataformas con una pantalla de prueba, y la CI está en marcha.
Todavía no se puede jugar.

La hoja de ruta está en los [hitos de GitHub](https://github.com/jesuslcorominas/wwii/milestones):

| Hito | Contenido |
|---|---|
| M0 · Fundamentos | Proyecto, CI, mapa y datos base |
| M1 · Motor de reglas | Reglas completas en `core` con ambos reglamentos |
| M2 · Partida contra la IA (MVP) | Juego completo en local contra la IA y hotseat |
| M3 · Escenarios | Modo rápido por escenarios y equilibrio de la campaña |
| M4 · Publicación | Identidad, arte, Firebase base y publicación en tiendas |
| M5 · Multijugador online | Partidas online asíncronas por código y aleatorias |

Los cambios por versión se recogen en el [CHANGELOG](CHANGELOG.md).

## Plataformas

| Plataforma | Lanzador | Estado |
|---|---|---|
| Android (8.0, API 26, o superior) | `androidApp` | Prioritaria |
| iOS (18.5 o superior) | `iosApp` (proyecto Xcode) | Prioritaria |
| Web (Kotlin/Wasm, navegador moderno) | `webApp` | Después |
| Escritorio (JVM: macOS, Windows, Linux) | `desktopApp` | Después |

## Requisitos

- **JDK 17.** Gradle lo usa como toolchain; el wrapper (`./gradlew`) descarga Gradle por sí mismo.
- **Android SDK** con la plataforma 37 (Android Studio lo instala). Si no usas Android Studio, crea
  `local.properties` en la raíz con `sdk.dir=<ruta al SDK>`.
- **Xcode** con un simulador de iOS 18.5 o superior, solo para iOS y solo en macOS.
- **Chrome**, para los tests del motor en Wasm (corren en Chrome sin interfaz).

Versiones de Kotlin, Compose Multiplatform y del resto de dependencias: en
[`gradle/libs.versions.toml`](gradle/libs.versions.toml).

## Compilar y ejecutar

Todos los comandos se lanzan desde la raíz del repo.

```bash
./gradlew build
```

Compila todos los targets y pasa los tests y el lint de Android. Las primeras ejecuciones tardan porque
descargan dependencias y la toolchain de Kotlin/Native.

### Android

Con un emulador arrancado o un dispositivo conectado:

```bash
./gradlew :androidApp:installDebug
```

Solo el APK de depuración (queda en `androidApp/build/outputs/apk/debug/`):

```bash
./gradlew :androidApp:assembleDebug
```

### iOS

Abre `iosApp/iosApp.xcodeproj` en Xcode y ejecuta el esquema `iosApp` en un simulador. La fase
"Compile Kotlin Framework" del proyecto compila el módulo `ui` como framework (`WwiiUi`) con
`:ui:embedAndSignAppleFrameworkForXcode`, así que no hace falta lanzar Gradle antes.

Desde la terminal, sin firmar y para el simulador:

```bash
xcodebuild -project iosApp/iosApp.xcodeproj -scheme iosApp -configuration Debug -destination 'generic/platform=iOS Simulator' CODE_SIGNING_ALLOWED=NO build
```

Para instalarla en un simulador concreto, compila con
`-destination 'platform=iOS Simulator,name=<dispositivo>'` e instala con
`xcrun simctl install booted <ruta a WWII.app>`.

### Escritorio

```bash
./gradlew :desktopApp:run
```

### Editor del mapa

Herramienta interna de escritorio para digitalizar el mapa (no se distribuye):

```bash
./gradlew :mapeditor:run
```

Con un mapa ya existente: `./gradlew :mapeditor:run --args="<ruta al mapa>.json"`. Uso en
[docs/editor-mapa.md](docs/editor-mapa.md).

### Web

```bash
./gradlew :webApp:wasmJsBrowserDevelopmentRun
```

Sirve la app en <http://localhost:8080>.

### Tests, lint y cobertura

```bash
./gradlew :core:allTests
```

Pasa los tests del motor en JVM, simulador de iOS y Wasm. `./gradlew :core:jvmTest` es lo más rápido
para iterar. Los del editor del mapa: `./gradlew :mapeditor:test`.

```bash
./gradlew ktlintCheck
```

Es el mismo lint que la CI; `./gradlew ktlintFormat` corrige lo que puede.

```bash
./gradlew koverHtmlReport
```

Genera la cobertura del motor en `build/reports/kover/html/index.html` (`koverXmlReport` genera el XML
que lee la CI).

## Estructura

```
core/         Motor del juego en Kotlin puro (solo commonMain): sin Compose, Firebase ni APIs de plataforma
ui/           Pantallas en Compose Multiplatform, compartidas por todas las plataformas
androidApp/   Lanzador de Android
iosApp/       Lanzador de iOS (proyecto Xcode, no es módulo de Gradle)
desktopApp/   Lanzador de escritorio (JVM)
webApp/       Lanzador web (Kotlin/Wasm)
mapeditor/    Editor del mapa: herramienta interna de escritorio (JVM), no se distribuye
docs/         Arquitectura y registro de decisiones
```

`ui` depende de `core` y cada lanzador depende de `ui`; `mapeditor` depende solo de `core`, y `core` no
depende de nada del proyecto. El
detalle, en [docs/arquitectura.md](docs/arquitectura.md).

## Documentación

- [Arquitectura](docs/arquitectura.md): módulos, motor determinista, reglamentos y capa de datos.
- [Mapa: modelo y formato JSON](docs/formato-mapa.md): coordenadas, territorios, formato del fichero
  del mapa y validación.
- [Editor del mapa](docs/editor-mapa.md): cómo calibrar la rejilla sobre una imagen y pintar el mapa.
- [Registro de decisiones](docs/decisiones/README.md): una nota (ADR) por cada decisión vigente.
- [Cómo contribuir](CONTRIBUTING.md): ramas, pull requests, commits, lint y tests.
- [CHANGELOG](CHANGELOG.md): cambios por versión.

## CI

GitHub Actions:

- **PR a `develop`** ([`pr-checks.yml`](.github/workflows/pr-checks.yml)): ktlint, tests del motor
  con comentario de cobertura, build de iOS en simulador y build completo en macOS cuando la PR toca
  código que lo necesita. El job **`CI Gate`** resume el resultado y es el check obligatorio para
  mergear.
- **PR a `main`** ([`release.yml`](.github/workflows/release.yml)): job **`Tests & Lint`**, obligatorio
  en `main`.
- **Releases**: un push a `release/**` abre la PR a `main`; al mergearla se crea el GitHub Release
  `v<versión>` y una PR de sincronización `main → develop`.

Más detalle en [CONTRIBUTING.md](CONTRIBUTING.md).

## Licencia

Copyright © 2026 Jesús López Corominas. **Todos los derechos reservados.**

El código es público para que se pueda leer, pero no es software libre ni de código abierto: no se
concede permiso para usarlo, copiarlo, modificarlo ni distribuirlo. Ver [LICENSE](LICENSE).
