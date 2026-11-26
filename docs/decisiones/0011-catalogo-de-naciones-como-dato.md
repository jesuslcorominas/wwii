# 0011. Catálogo de naciones como dato y carpeta `data/`

- **Estado:** Aceptada
- **Fecha:** 2026-11-26

## Contexto

El motor necesita saber qué naciones hay, con qué bando empieza cada una y cuántas fichas tiene.
El mapa no guarda el bando ([0009](0009-modelo-del-mapa-hexagonal.md)), así que alguien tiene que
aportarlo. Además, no queremos suponer un único teatro: más adelante podría haber otros mapas con
otras naciones. Y el motor no lee ficheros ([0002](0002-motor-puro-y-determinista.md)), pero los
datos reales tienen que vivir en algún sitio del repo donde se puedan validar.

## Decisión

- **El catálogo es un dato**: un JSON con `formatVersion` que `core` carga con `CatalogJson.decode`
  y valida entero, igual que el mapa. No hay un catálogo global en el código: cada mapa o escenario
  puede traer el suyo.
- **El catálogo aporta el bando inicial** con `sideOf: (NationId) -> Side`. Los cambios de bando
  durante la partida son cosa del estado de partida.
- **Fichas por cantidad, no por valor.** El catálogo dice cuántas fichas tiene cada nación de cada
  tipo; los factores de combate y movimiento los pone el reglamento. Las fichas individuales
  reciben un id determinista (`DEU-L01`) para que el estado de partida y el replay las referencien.
- **Naciones identificadas por su código ISO 3166 alfa-3** (`SUN` para la URSS), el mismo que
  usan las fichas del diseño y que usarán los territorios del mapa.
- **Validación cruzada aparte** (`CatalogMapValidator`): el catálogo y el mapa son válidos cada uno
  por su lado, y una tercera pieza comprueba que encajan.
- **Carpeta `data/` en la raíz** para los datos del juego (catálogos, mapas y, más adelante,
  reglamentos y escenarios). `core` no la lee: la leen la interfaz y los tests JVM, que reciben su
  ruta de Gradle y validan los ficheros reales en la CI.

El detalle está en [Catálogo de naciones: modelo y formato JSON](../formato-catalogo.md).

## Alternativas consideradas

- **Catálogo como objeto Kotlin en el código:** cierra la puerta a otros teatros y mezcla datos con
  lógica.
- **Fichas dentro de cada nación** (`"counters": {"land": 30}`): más compacto, pero la lista aparte
  permite validar referencias como hace el mapa con sus territorios y añadir campos por ficha.
- **Factores de combate en la ficha:** el juego de referencia no los imprime y dependen del
  reglamento.
- **Datos en `commonMain/resources` o como Kotlin generado en la compilación:** no hay lectura de
  recursos común a todas las plataformas en `core`, y generar código añade una tarea de Gradle que
  hoy no hace falta.

## Consecuencias

- Añadir un teatro es añadir datos: un catálogo y un mapa nuevos, sin tocar el motor.
- El mapa de Europa tiene que usar los mismos `NationId` que el catálogo; `CatalogMapValidator` lo
  comprobará cuando exista.
- La interfaz tendrá que leer los ficheros de `data/` y pasarle el texto a `core`.
- Cualquier cambio incompatible del formato obliga a subir `formatVersion`.
