# 0001. Kotlin Multiplatform y Compose Multiplatform para las cuatro plataformas

- **Estado:** Aceptada
- **Fecha:** 2026-11-25

## Contexto

El juego tiene que llegar a Android e iOS, que son las plataformas prioritarias, y después a web y
escritorio. Lo desarrolla una sola persona, así que mantener cuatro bases de código no es viable. El
motor de reglas es la parte más compleja y tiene que comportarse exactamente igual en todas las
plataformas: una partida online entre un Android y un iPhone solo funciona si los dos clientes
calculan el mismo resultado.

## Decisión

- El proyecto usa **Kotlin Multiplatform (KMP)** con un único código para el motor (`core`) y la
  interfaz (`ui`).
- La interfaz se hace con **Compose Multiplatform** y se comparte entera entre Android, iOS, web
  (Kotlin/Wasm) y escritorio (JVM).
- Cada plataforma tiene un lanzador fino (`androidApp`, `iosApp`, `webApp`, `desktopApp`) que solo
  arranca la interfaz compartida.
- El código vive en `commonMain` siempre que sea posible. El código específico de plataforma se
  limita a lo que no se puede escribir en común: el arranque de la app y, más adelante, la
  integración con servicios externos (ver [0004](0004-sin-backend-propio.md)).

## Alternativas consideradas

- **Apps nativas por plataforma (Kotlin + Swift + web):** el motor y la interfaz se escribirían varias
  veces y sería difícil garantizar que el motor calcula lo mismo en todas.
- **KMP solo para el motor, con interfaz nativa en cada plataforma:** comparte lo crítico, pero sigue
  obligando a hacer cuatro interfaces para un tablero hexagonal complejo.
- **Flutter u otro framework multiplataforma ajeno a Kotlin:** obligaría a escribir el motor en otro
  lenguaje y descartaría la experiencia previa en Kotlin y KMP.

## Consecuencias

- Un único motor y una única interfaz para las cuatro plataformas.
- `core` se compila y se prueba en JVM, iOS y Wasm, y la CI lo comprueba en cada PR.
- Web y escritorio salen casi sin coste extra, aunque se prioricen después.
- El build de iOS depende de Kotlin/Native y de Xcode: es lento y necesita runners de macOS en la CI.
- Las dependencias tienen que ser compatibles con los targets que use cada módulo. Si una librería no
  soporta Wasm o Kotlin/Native, se envuelve detrás de una interfaz o se busca otra.
