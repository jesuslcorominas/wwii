# Arquitectura

Visión técnica del proyecto: cómo se reparte el código, qué principios sigue el motor y dónde encajan
los servicios externos. Las decisiones que justifican cada punto están en el
[registro de decisiones](decisiones/README.md).

> Estado: hoy el repo es un esqueleto. Los módulos y sus dependencias ya existen tal como se
> describen aquí. El modelo del juego, los reglamentos y la capa de datos todavía no: este documento
> recoge cómo se van a construir y se irá completando a medida que entren.

## Módulos

```
               ┌──────────────┐
               │     core     │  Motor del juego. Kotlin puro (commonMain)
               └──────▲───────┘
                      │
               ┌──────┴───────┐
               │      ui      │  Pantallas en Compose Multiplatform (commonMain)
               └──────▲───────┘
       ┌──────────┬───┴──────┬───────────┐
  androidApp   iosApp    desktopApp    webApp     Lanzadores por plataforma
```

| Módulo | Qué contiene | Targets |
|---|---|---|
| `core` | Motor del juego: modelo, mapa hexagonal, reglamentos, combate, refuerzos, victoria, IA y serialización de partidas | JVM, Android, iOS (arm64 y simulador), Wasm |
| `ui` | Tablero, pantallas y menús en Compose Multiplatform. En iOS se compila como el framework estático `WwiiUi` | JVM, Android, iOS (arm64 y simulador), Wasm |
| `androidApp` | `Activity` que muestra la interfaz de `ui` | Android |
| `iosApp` | Proyecto Xcode con SwiftUI que embebe el framework `WwiiUi`. No es un módulo de Gradle | iOS |
| `desktopApp` | Ventana de escritorio con Compose for Desktop | JVM |
| `webApp` | Página web que carga la interfaz compilada a Wasm | Wasm |
| `mapeditor` | Editor del mapa: herramienta interna de escritorio para digitalizar el mapa. No se distribuye ([Editor del mapa](editor-mapa.md)) | JVM |

Reglas de dependencia:

- `core` no depende de ningún otro módulo ni de Compose, Firebase, AndroidX o APIs de plataforma.
- `ui` depende de `core`. Los lanzadores dependen de `ui` y no contienen lógica del juego.
- `mapeditor` depende solo de `core` (modelo, validación y JSON del mapa), nunca de `ui`.
- El código va en `commonMain` siempre que sea posible.

Ver [0001. Kotlin Multiplatform y Compose Multiplatform](decisiones/0001-kotlin-multiplatform-y-compose.md).

## Motor (`core`)

### Puro y determinista

El motor es una función de estado y acción:

```
estado inicial + reglamento + semilla
        │
        ▼
  acción 1 ──▶ motor valida y aplica ──▶ estado 1
  acción 2 ──▶ motor valida y aplica ──▶ estado 2
  ...
```

- **Estado inmutable.** Cada acción válida produce un estado nuevo y nunca se modifica el anterior.
- **Acciones.** Toda jugada, sea de un humano o de la IA, es una acción que el motor valida antes de
  aplicarla. Una acción ilegal se rechaza.
- **Aleatoriedad con semilla.** Las tiradas de dados salen de un generador con semilla inyectada.
  Nunca `Random` sin semilla ni la hora del sistema.
- **Replay.** Una partida se guarda como estado inicial, reglamento, semilla y lista de acciones.
  Reproducir la lista da siempre el mismo estado final, en cualquier plataforma. Es la base de guardar
  y cargar, de deshacer y de validar las partidas online.

Ver [0002. Motor del juego en Kotlin puro y determinista](decisiones/0002-motor-puro-y-determinista.md).

### Mapa

- Mapa hexagonal con coordenadas axiales (paquete `core/.../map`). `core` no conoce la orientación
  del hex ni los píxeles.
- Terreno, ríos (por lado de hexágono), estrechos, ciudades y territorios son datos, no código. La
  costa se deduce del terreno.
- El mapa agrupa los territorios por nación. El bando de cada nación no está en el mapa: lo aportan el
  catálogo de naciones o el estado de la partida.
- Se carga desde JSON con `MapJson.decode`, que valida el mapa entero. El motor y el juego solo
  usan `HexMap`; `MapDraft` (un mapa sin validar) es solo para el editor del mapa.

Modelo, formato JSON y reglas de validación en [Mapa: modelo y formato JSON](formato-mapa.md). Ver
[0009. Modelo del mapa hexagonal](decisiones/0009-modelo-del-mapa-hexagonal.md).

### Catálogo de naciones

- Paquete `core/.../catalog`: naciones, rol (principal, aliado menor o neutral), bando inicial y
  número de fichas por tipo (tierra, naval y aérea). Las fichas no llevan factores: los pone el
  reglamento.
- Es un dato, no un objeto global. Se carga desde JSON con `CatalogJson.decode` y aporta al mapa el
  bando inicial con `sideOf`.
- `CatalogMapValidator` comprueba que un catálogo y un mapa encajan.
- Los datos reales viven en `data/` (en la raíz del repo). `core` no los lee: recibe el texto. Los
  tests JVM validan los ficheros reales.

Modelo, formato y validación en [Catálogo de naciones: modelo y formato JSON](formato-catalogo.md).
Ver [0011. Catálogo de naciones como dato](decisiones/0011-catalogo-de-naciones-como-dato.md).

### Reglamentos

Cada partida se juega con el reglamento **Clásico** o el **Avanzado**. El reglamento es un objeto de
configuración y estrategia que forma parte del estado inicial. Las reglas que difieren se modelan como
opciones o estrategias de ese objeto, nunca con `if (classic)` repartidos por el motor.

Ver [0003. Reglamento configurable](decisiones/0003-reglamento-configurable.md).

### Modos de juego

- **Campaña:** 18 turnos, toda Europa y el norte de África.
- **Escenarios:** 4 a 6 turnos en una región concreta. Son datos (estado inicial, región, turnos y
  condiciones de victoria) que usan el mismo motor.

Ver [0006. Escenarios cortos como modo rápido](decisiones/0006-escenarios-como-modo-rapido.md).

## Interfaz (`ui`)

- Compose Multiplatform compartido por las cuatro plataformas.
- La interfaz muestra el estado que expone el motor y le envía acciones. No decide reglas.
- El layout se elige por tamaño de ventana, no por orientación:
  - Menús en columna estrecha, fijados en vertical en móvil.
  - Tablero en las dos orientaciones: horizontal con panel lateral como diseño principal, y vertical
    con panel inferior.

Ver [0007. Layout por tamaño de ventana](decisiones/0007-layout-por-tamano-de-ventana.md).

## Datos y servicios externos

- No hay backend propio. Los servicios remotos (Auth, Firestore, Hosting, Remote Config, Crashlytics)
  serán de Firebase en su plan gratuito.
- Firebase vive solo en la capa de datos, detrás de interfaces propias (por ejemplo,
  `GameRepository`) con una implementación por plataforma. `core` no lo conoce.
- El juego local, contra la IA o en hotseat, funciona sin red.
- En el online asíncrono, la partida avanza por turnos con un plazo configurable. Si vence el plazo,
  ese bando pierde el turno. Las reglas de seguridad de Firestore y la validación por replay en cada
  cliente sustituyen al servidor.

Ver [0004. Sin backend propio](decisiones/0004-sin-backend-propio.md) y
[0005. Online asíncrono con plazo por turno](decisiones/0005-online-asincrono-con-plazo.md).

## Testing

- **`core`:** TDD con `kotlin.test` en `commonTest`, ejecutado en JVM, simulador de iOS y Wasm.
  - Sin librerías de mocking. Se construyen estados reales mínimos.
  - Las dependencias externas se sustituyen por implementaciones falsas escritas a mano.
  - La aleatoriedad se controla con una semilla fija.
- **Partidas completas:** se prueban por replay. Estado inicial, semilla y lista de acciones tienen
  que dar el estado final esperado.
- **Cobertura:** se mide solo sobre `core`, con Kover.
- **`ui` y lanzadores:** sus tests son opcionales mientras la lógica viva en `core`.

Comandos en el [README](../README.md#tests-lint-y-cobertura) y normas en
[CONTRIBUTING](../CONTRIBUTING.md#tests).
