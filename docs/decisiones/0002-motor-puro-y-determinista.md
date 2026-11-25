# 0002. Motor del juego en Kotlin puro y determinista

- **Estado:** Aceptada
- **Fecha:** 2026-11-25

## Contexto

El motor aplica todas las reglas: movimiento, combate, suministro, refuerzos, victoria e IA. Tiene que:

- Funcionar igual en las cuatro plataformas y sin conexión.
- Probarse a fondo con tests rápidos y sin dispositivo.
- Permitir partidas online sin servidor propio ([0004](0004-sin-backend-propio.md)). Cada cliente
  tiene que poder comprobar que las jugadas del rival son legales y que los dados no se han
  manipulado.

## Decisión

- **`core` es Kotlin puro**: solo `commonMain`, sin dependencias de Compose, Firebase ni APIs de
  plataforma.
- **Estado inmutable más acciones**: el estado de la partida es inmutable. Cada jugada es una acción
  que el motor valida y aplica para producir el estado siguiente.
- **Aleatoriedad con semilla inyectada**: toda tirada de dados pasa por un generador de números
  aleatorios con semilla que recibe el motor. Nunca `Random` sin semilla ni la hora del sistema.
- **Partida = estado inicial + semilla + lista de acciones.** Reproducir esa lista (*replay*) desde el
  estado inicial da siempre el mismo estado final, en cualquier plataforma.

## Alternativas consideradas

- **Estado mutable dentro del motor:** es más directo de escribir, pero complica deshacer jugadas, el
  replay y los tests, y hace más fácil que dos clientes acaben con estados distintos.
- **Aleatoriedad del sistema:** no se puede reproducir ni verificar. Una partida online dependería de
  confiar en el cliente que tira los dados.
- **Guardar instantáneas del estado en vez de las acciones:** ocupa más, no permite verificar la
  legalidad de cada jugada y pierde el historial de la partida.

## Consecuencias

- Las partidas son reproducibles y verificables: cada cliente puede rehacer la partida desde cero y
  detectar una jugada ilegal o un resultado manipulado.
- Los tests construyen un estado real mínimo, inyectan una semilla fija y comprueban un resultado
  determinista, sin mocks. El desarrollo en `core` es con TDD.
- Guardar, cargar y sincronizar una partida es serializar estado inicial, semilla y acciones.
- La IA y la interfaz trabajan sobre el mismo modelo de acciones que un jugador humano.
- Cualquier dependencia de `core` tiene que ser multiplataforma y no puede introducir aleatoriedad ni
  estado global.
