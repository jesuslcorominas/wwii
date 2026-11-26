# Cómo contribuir

Esta guía explica cómo se trabaja en el repo: ramas, pull requests, checks, commits, lint, tests y
documentación. Para compilar y ejecutar el proyecto, ver el [README](README.md).

El repositorio es público, pero el código no tiene licencia de código abierto (ver [LICENSE](LICENSE)).
Las contribuciones externas no están abiertas por ahora; si encuentras un fallo o tienes una idea,
abre una issue con las plantillas del repo.

## Ramas

| Rama | Para qué |
|---|---|
| `develop` | Rama base. Todo el trabajo sale de aquí y vuelve aquí por PR. |
| `main` | Releases. Solo recibe PR desde `release/*` y la sincronización posterior. |
| `release/*` | Preparación de una versión, por ejemplo `release/0.2.0`. Sale de `develop`. |
| Ramas de trabajo | Una por tarea, desde `develop`, con un prefijo según el tipo: `feature/`, `fix/`, `chore/`, `docs/`... |

`develop` y `main` están protegidas: no admiten push directo, force push ni borrado, y nada entra
en ellas sin PR.

## Pull requests

- **Siempre contra `develop`**, nunca contra `main`:

  ```bash
  gh pr create --base develop
  ```

- Una PR por tarea, enlazando la issue que resuelve (`Closes #N`).
- Rellena la [plantilla de PR](.github/PULL_REQUEST_TEMPLATE.md), incluido el checklist.
- La rama se borra sola al mergear la PR.

### Checks obligatorios

| Rama destino | Check obligatorio | Qué comprueba |
|---|---|---|
| `develop` | `CI Gate` | Resume [`pr-checks.yml`](.github/workflows/pr-checks.yml): lint, tests del motor y build de iOS en simulador |
| `main` | `Tests & Lint` | [`release.yml`](.github/workflows/release.yml): lint y tests del motor |

En las PR a `develop`, los jobs de macOS solo corren si hacen falta:

- **iOS build (simulator)** corre cuando la PR toca `core`, `ui`, `iosApp`, el build de Gradle o el
  propio workflow. Es bloqueante.
- **Full build (macOS)** (`./gradlew build`) corre cuando la PR toca `ui`, los lanzadores, el build de
  Gradle o el propio workflow. De momento no bloquea, pero un fallo significa que `./gradlew build`
  está roto y hay que arreglarlo.

Una PR que solo cambia documentación o plantillas pasa con lint y tests en Linux.

La CI comenta la cobertura del motor en la PR. Es informativa: no bloquea `CI Gate`.

## Commits

Mensajes en español con el formato de [Conventional Commits](https://www.conventionalcommits.org/es/v1.0.0/):

```
<tipo>: <descripción en minúscula, sin punto final>
```

Tipos habituales:

| Tipo | Uso |
|---|---|
| `feat` | Funcionalidad nueva |
| `fix` | Corrección de un fallo |
| `docs` | Solo documentación |
| `test` | Tests nuevos o corregidos, sin cambiar código de producción |
| `refactor` | Cambio de código que no altera el comportamiento |
| `ci` | Workflows y plantillas de GitHub |
| `chore` | Mantenimiento: dependencias, build, configuración |

Ejemplos del historial:

```
feat: esqueleto del proyecto KMP con core, ui y lanzadores
ci: portar workflows y plantillas desde teamflowmanager
```

Cada commit debe ser una unidad coherente que se pueda revisar y revertir por separado. No se usa
`--no-verify` ni se reescribe el historial de `develop` ni de `main`.

## Lint

El estilo de Kotlin lo controla ktlint (configuración en [`.editorconfig`](.editorconfig)):

```bash
./gradlew ktlintCheck
```

Es el mismo check que la CI. Para corregir lo que se pueda de forma automática:

```bash
./gradlew ktlintFormat
```

## Tests

```bash
./gradlew :core:allTests
```

Pasa los tests del motor en JVM, simulador de iOS y Wasm (Chrome sin interfaz). Para iterar,
`./gradlew :core:jvmTest` es lo más rápido.

Reglas:

- **TDD en `core`.** Toda regla del juego entra con su test en `core/src/commonTest`: primero el test
  que falla y después la implementación mínima que lo pasa.
- Los tests de `commonTest` tienen que pasar en todos los targets, no solo en JVM.
- Sin librerías de mocking en `core`. Las dependencias externas van detrás de interfaces propias y en
  los tests se sustituyen por implementaciones falsas escritas a mano.
- La aleatoriedad nunca se mockea: se inyecta un generador con semilla fija y se comprueba el
  resultado, que es determinista.
- Ficheros `<Clase>Test.kt` en el mismo paquete que la clase probada; nombres de test en camelCase que
  describen el comportamiento (`campaignHasEighteenTurns`).

En `ui` y en los lanzadores los tests son opcionales mientras la lógica viva en `core`.

Cobertura del motor:

```bash
./gradlew koverHtmlReport
```

## Documentación

La documentación forma parte de la tarea. Toda PR que cambie cómo se construye, se ejecuta, se juega o
se estructura el proyecto actualiza en la misma PR lo que corresponda:

- [`README.md`](README.md): qué es, estado, plataformas, requisitos, cómo compilar y ejecutar,
  estructura.
- [`docs/`](docs/arquitectura.md): arquitectura, formatos de datos y features complejas.
- [`docs/decisiones/`](docs/decisiones/README.md): un ADR nuevo por cada decisión de arquitectura o de
  producto, o uno que sustituye a otro si cambia una decisión vigente.
- [`CHANGELOG.md`](CHANGELOG.md): los cambios visibles van a la sección "Sin publicar".

Las reglas del juego se redactan desde cero. No se copian textos, tablas, mapas ni fichas de ningún
juego publicado.

## Releases

1. Crea `release/<versión>` desde `develop` y actualiza `app.versionName` y `app.versionCode` en
   `gradle.properties` y el [CHANGELOG](CHANGELOG.md).
2. Al hacer push de la rama, la CI abre la PR a `main`. Esa PR tiene que pasar `Tests & Lint`.
3. Al mergearla, la CI crea el GitHub Release `v<versión>` y abre una PR de sincronización
   `main → develop` con auto-merge.
