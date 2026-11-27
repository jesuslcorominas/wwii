# 0012. Mapa generado desde geografía de dominio público, con ids de ciudad estables

- **Estado:** Aceptada
- **Fecha:** 2026-11-27

## Contexto

El juego necesita un tablero propio de Europa y el norte de África en 1939: unos 2000 hexes con
terreno, territorios, ríos, estrechos y ciudades. No puede calcar el tablero de ningún juego
publicado ([0008](0008-repo-publico-todos-los-derechos-reservados.md)), tiene que salir de geografía
real (D2) y hay que poder rehacerlo cuando cambie una regla de diseño. Además, otros datos (el
despliegue inicial, el reglamento) necesitan referirse a ciudades concretas, y el modelo del mapa
([0009](0009-modelo-del-mapa-hexagonal.md)) solo les daba nombre.

## Decisión

- **El mapa se genera.** Un script de Python con solo la biblioteca estándar (`tools/mapgen/`)
  construye `data/maps/europe-1939.json` a partir de Natural Earth 5.1.2 (dominio público) y de un
  módulo de datos de diseño propio: territorios, fronteras de 1939 como polígonos, ciudades,
  estrechos y retoques por hex. La salida es determinista y está en la forma canónica del editor.
- **Natural Earth no se versiona.** Se descarga de una etiqueta fija y se comprueba con SHA-256,
  como explica [el documento del mapa](../mapa-europa-1939.md).
- **El generador es la fuente.** `:mapeditor` sirve para revisar y probar; los retoques que se quieren
  conservar se pasan a los datos del generador.
- **Toda ciudad tiene un id estable** (`CityId`): un slug en ASCII, en minúsculas y con guiones,
  único en el mapa (`suez`, `brest-litovsk`). Otros datos citan las ciudades por él, y el nombre
  visible puede cambiar sin romperlos. El validador del mapa rechaza ids mal formados
  (`InvalidCityId`) y repetidos (`DuplicateCityId`), y el del despliegue, ciudades que no existen
  (`UnknownCity`).
- **`formatVersion` sigue en 1** aunque `city.id` pasa a ser obligatorio: no había ningún mapa
  publicado con el formato anterior y no hay nada que migrar.

## Alternativas consideradas

- **Digitalizar a mano en `:mapeditor`**: lento con 2000 hexes, no se puede reproducir y cada cambio
  de criterio obliga a repintar.
- **Generador en Kotlin dentro de Gradle**: un módulo más que mantener para una herramienta que se
  ejecuta pocas veces. Python sin dependencias es lo que ya usan las herramientas de diseño.
- **Versionar Natural Earth**: son unos 20 MB de datos que se pueden descargar de una versión fija.
- **Citar ciudades por nombre o por coordenada**: el nombre cambia con la traducción y la coordenada,
  si se retoca el mapa.
- **Subir `formatVersion` a 2**: obligaría a mantener la lectura de la versión 1 sin que exista
  ningún fichero en ella.

## Consecuencias

- El mapa se puede regenerar idéntico y revisar con `git diff`.
- Cambiar una frontera o añadir una ciudad es editar datos y regenerar, no repintar.
- Los ids de territorio y de ciudad son contrato con otros datos: un test fija los de territorio y el
  despliegue valida los de ciudad.
- Un retoque hecho solo en el editor se pierde al regenerar, salvo que se pase al generador.
