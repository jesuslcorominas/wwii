# Mapa: modelo y formato JSON

Cómo representa `core` el tablero y cómo se guarda en JSON. El código está en
`core/src/commonMain/kotlin/com/jesuslcorominas/wwii/core/map/` (más los tipos compartidos de
`core/.../nation/`). Las decisiones de fondo están en
[0009. Modelo del mapa hexagonal](decisiones/0009-modelo-del-mapa-hexagonal.md).

## Qué hace `core` y qué no

| `core` | `ui` y editor del mapa |
|---|---|
| Coordenadas, vecinos, distancias, líneas, anillos | Orientación del hex (plano o en punta) y tamaño en píxeles |
| Terreno, ciudades, territorios, ríos, estrechos | Conversión entre coordenadas y píxeles, dibujo |
| Validación del mapa | Leer el fichero del mapa (recursos de Compose, disco en el editor) |
| `MapJson.decode(texto)` / `MapJson.encode(mapa)` | Pasar el texto a `core` y guardar el que devuelve |
| Borradores sin validar, solo para el editor (`MapDraft`, `MapJson.decodeDraft`) | Mostrar en el editor los problemas del borrador |

`core` no lee ficheros: recibe el texto del JSON. Así sigue siendo Kotlin puro, sin APIs de
plataforma.

## Coordenadas

Coordenadas **axiales** `(q, r)`. La tercera coordenada cúbica se deriva: `s = -q - r`.

- Las seis direcciones son índices `0..5`, sin nombres de brújula, porque "norte" o "noreste"
  dependen de la orientación con que se dibuje el hex. Índices consecutivos son lados consecutivos
  del hex, y `d` y `d + 3` son opuestos.

  | Dirección | 0 | 1 | 2 | 3 | 4 | 5 |
  |---|---|---|---|---|---|---|
  | `(Δq, Δr)` | `(1, 0)` | `(1, -1)` | `(0, -1)` | `(-1, 0)` | `(-1, 1)` | `(0, 1)` |

- `distanceTo`: número de pasos entre dos hexes, `max(|Δq|, |Δr|, |Δs|)`.
- `lineTo`: hexes de una recta, extremos incluidos. Cuando la recta pasa justo por el borde entre dos
  hexes, el desempate es fijo, así que el resultado es el mismo en todas las plataformas.
- `ring(n)` y `range(n)`: hexes a distancia exacta `n` y hasta `n`.
- `HexEdge`: el lado común de dos hexes adyacentes. Es el mismo sea cual sea el orden en que se nombren los dos hexes.

## Modelo

El mapa (`HexMap`) es un conjunto disperso de hexes: no tiene que ser un rectángulo, y lo que no está
en la lista está fuera del tablero. `neighborsInMap` solo devuelve vecinos del tablero.

Un `HexMap` siempre está validado: solo se crea con `HexMap.build(...)` o con `MapJson.decode(...)`,
y los dos lanzan `InvalidMapException` si algo no cuadra.

### Hex

| Campo | Significado |
|---|---|
| `terrain` | `land` (tierra), `sea` (mar) o `lake` (lago: intransitable y no navegable) |
| `territory` | Territorio al que pertenece. Obligatorio en tierra, prohibido en mar y lago |
| `mountain` | Montaña. Solo en tierra |
| `city` | Ciudad: nombre y tipo `city`, `territory_capital` o `national_capital`. Solo en tierra |
| `airport` | Aeropuerto sin ciudad. Toda ciudad o capital tiene aeropuerto sin necesidad de marcarlo |
| `strait` | Estrecho. Solo en mar; ver abajo |

La **costa no se guarda**, se deduce: un hex de tierra es costero si al menos un vecino del tablero
es de mar. Los lagos no hacen costa y el borde del tablero tampoco.

### Ríos

Un río va por un **lado** de hex, entre dos hexes de tierra adyacentes. Se consulta con
`hasRiver(a, b)`.

### Estrechos

Un estrecho es un **hex de mar** que declara las dos orillas de tierra que une. Al ser un hex de mar:

- los barcos lo atraviesan como cualquier otro mar y pueden quedarse en él;
- una unidad en una de las orillas lo controla con su zona de control sin reglas especiales.

Si una unidad terrestre puede cruzar de una orilla a otra, y cómo, lo decide el reglamento (con
transporte, o con un barco que hace de puente en el Avanzado). El mapa solo informa:
`isStrait(coord)` y `straitShores(coord)`.

### Territorios, naciones y bandos

- Un **territorio** es una unidad de tierra que se conquista entera: un país (`homeland`), una
  colonia (`colony`) o una posesión (`possession`). Pertenece a una **nación** (`NationId`, por
  ejemplo `"FRA"`; en el mapa de Europa, el código ISO 3166 alfa-3 del catálogo).
- Una nación puede tener varios territorios separados (su país, sus colonias): no hace falta declarar
  nada.
- Los hexes de **un mismo territorio** tienen que estar conectados por tierra o por un estrecho (sus
  dos orillas cuentan como unidas). Si un territorio tiene islas o exclaves, se marca con
  `"discontiguous": true`.
- **El mapa no guarda el bando.** El bando de una nación depende del reglamento y cambia durante la
  partida (los neutrales entran en guerra), así que lo aporta el catálogo de naciones o el estado de
  la partida. El mapa agrupa por nación (`nations()`, `territoriesOf(nation)`, `hexesOf(territory)`)
  y agrupa por bando con una función que le pasa quien conoce los bandos:
  `territoriesBySide { nation -> side }`.

Tipos compartidos con el catálogo de naciones, en `core/.../nation/`:

- `NationId`: identificador de nación (texto no vacío).
- `Side`: `AXIS`, `WESTERN_ALLIES`, `USSR`, `NEUTRAL`.

El catálogo de naciones (nombres, bando inicial, fichas) usa estos tipos y los identificadores de
nación del mapa. El mapa no comprueba que cada `nation` exista en el catálogo: lo hace
`CatalogMapValidator` (ver [Catálogo de naciones: modelo y formato JSON](formato-catalogo.md)).

## Formato JSON

```json
{
  "formatVersion": 1,
  "id": "test-small",
  "name": "Mapa de prueba",
  "territories": [
    { "id": "AA", "nation": "AA", "kind": "homeland", "name": "Alfa", "discontiguous": true },
    { "id": "BB", "nation": "BB", "kind": "homeland", "name": "Bravo" },
    { "id": "BC", "nation": "BB", "kind": "colony", "name": "Colonia de Bravo" }
  ],
  "hexes": [
    { "q": 0, "r": 0, "terrain": "land", "territory": "AA", "city": { "name": "Alfa", "kind": "national_capital" } },
    { "q": 1, "r": 0, "terrain": "land", "territory": "AA", "mountain": true },
    { "q": 3, "r": 1, "terrain": "land", "territory": "BB", "airport": true },
    { "q": 4, "r": 1, "terrain": "land", "territory": "BB", "city": { "name": "Puerto Bravo" } },
    { "q": 1, "r": 1, "terrain": "lake" },
    { "q": 2, "r": 2, "terrain": "sea", "strait": [[2, 1], [2, 3]] },
    { "q": 5, "r": 0, "terrain": "sea" }
  ],
  "rivers": [
    { "a": [2, 0], "b": [3, 0] }
  ]
}
```

(Extracto: el mapa de prueba completo está en
`core/src/commonTest/kotlin/com/jesuslcorominas/wwii/core/map/TestMaps.kt`.)

Reglas del formato:

- `formatVersion` es un entero en la raíz y es obligatorio. Hoy solo existe la versión `1`. Un mapa
  con otra versión, o sin ella, se rechaza con un error claro antes de leer nada más. Cualquier cambio
  del formato que no sea compatible sube este número.
- Las claves desconocidas se rechazan (un error de escritura como `"montain"` no pasa en silencio).
  Por eso todo cambio del formato tiene que ir con su `formatVersion`.
- Los valores con su valor por defecto se omiten al escribir (`mountain: false`, `airport: false`,
  `city` y `territory` nulos, `kind: "city"`, `discontiguous: false`), para que el mapa completo
  ocupe poco.
- Coordenadas como pares `[q, r]`. Los lados (ríos) son pares de hexes, `{"a": [q, r], "b": [q, r]}`,
  nunca hex más dirección, para no depender de la orientación.
- Enums en minúsculas: `land`, `sea`, `lake`; `homeland`, `colony`, `possession`; `city`,
  `territory_capital`, `national_capital`.
- `MapJson.encode(mapa)` escribe el JSON compacto, y `MapJson.encode(mapa, prettyPrint = true)`
  con sangría. Leer lo escrito devuelve un mapa igual, y volver a escribirlo da el mismo texto.

## Validación

`MapJson.decode` y `HexMap.build` comprueban todo y devuelven **todos** los problemas a la vez en
`InvalidMapException.issues` (lista de `MapIssue`), no solo el primero:

| Regla | `MapIssue` |
|---|---|
| JSON válido, con `formatVersion` soportada, claves y valores conocidos | `MalformedDocument`, `UnsupportedFormatVersion` |
| Sin hexes ni territorios repetidos | `DuplicateHex`, `DuplicateTerritory` |
| Todo hex de tierra tiene un territorio que existe; mar y lago no tienen | `LandWithoutTerritory`, `UnknownTerritory`, `WaterWithTerritory` |
| Ciudad, aeropuerto y montaña solo en tierra | `LandFeatureOnWater` |
| Todo territorio tiene al menos un hex | `EmptyTerritory` |
| Territorio conectado (por tierra o estrecho) o marcado `discontiguous` | `DisconnectedTerritory` |
| Río entre dos hexes de tierra adyacentes del tablero, sin repetir | `InvalidRiver`, `DuplicateRiver` |
| Estrecho en un hex de mar, con dos orillas distintas, de tierra, adyacentes y en el tablero | `StraitNotOnSea`, `InvalidStraitShore` |
| Como mucho una capital de nación por nación, y en su país | `DuplicateNationalCapital`, `NationalCapitalOutsideHomeland` |
| Como mucho una capital de territorio por colonia o posesión, y nunca en un país | `DuplicateTerritoryCapital`, `TerritoryCapitalInHomeland` |

## Borradores (editor del mapa)

Un mapa a medio digitalizar casi nunca es válido (tierra sin territorio, territorios todavía vacíos…),
y aun así hay que poder guardarlo y volver a abrirlo. Para eso existe `MapDraft`: el contenido de un
mapa sin validar, con los mismos tipos del dominio (`Territory`, `Hex` y los ríos como pares de
`HexCoord`).

- `MapJson.decodeDraft(texto)` lee un borrador. Solo rechaza lo que no es un mapa (JSON roto,
  `formatVersion` no soportada, claves o valores desconocidos); un mapa que rompe las reglas se carga.
- `MapJson.encode(borrador, prettyPrint)` lo escribe en el mismo formato y con la misma
  `formatVersion`. `MapJson.encode(mapa)` es `encode(MapDraft.of(mapa))`.
- `borrador.issues()` devuelve todos los `MapIssue`, y `borrador.toMap()` construye el `HexMap` o lanza
  `InvalidMapException`.

**El motor y el juego solo consumen `HexMap`.** `MapDraft` es un formato de edición y de carga: nada
de `core` fuera del paquete `map` depende de él. Ver
[0010. Borradores de mapa para el editor](decisiones/0010-borradores-de-mapa-para-el-editor.md) y
[Editor del mapa](editor-mapa.md).
