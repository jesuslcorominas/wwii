# Catálogo de naciones: modelo y formato JSON

Qué naciones hay en un teatro, con qué bando empieza cada una y cuántas fichas tiene de cada tipo.
El código está en `core/src/commonMain/kotlin/com/jesuslcorominas/wwii/core/catalog/` y los datos
del teatro de Europa en [`data/catalogs/europe-1939.json`](../data/catalogs/europe-1939.json). Las
decisiones de fondo están en
[0011. Catálogo de naciones como dato y carpeta `data/`](decisiones/0011-catalogo-de-naciones-como-dato.md).

## Qué hace `core` y qué no

| `core` | Fuera de `core` |
|---|---|
| Modelo, validación y `CatalogJson.decode(texto)` / `CatalogJson.encode(catálogo)` | Leer el fichero y pasarle el texto (`ui`, tests JVM) |
| Bando inicial de cada nación: `sideOf` | Cambios de bando durante la partida (estado de partida) |
| Número de fichas por nación y tipo, e ids de cada ficha | Factores de combate y movimiento por tipo (reglamento) |
| Validación cruzada con un mapa | Cuándo entra cada nación y la tabla de refuerzos |

El catálogo es un dato, no un objeto global: cada mapa o escenario puede traer el suyo.

## Modelo

### Nación (`Nation`)

| Campo | Significado |
|---|---|
| `id` | `NationId`. En el teatro de Europa, código ISO 3166 alfa-3 (`DEU`, `GBR`…; `SUN` para la URSS), el que llevan las fichas y el que usarán los territorios del mapa de Europa |
| `name` | Nombre para mostrar |
| `role` | `major` (potencia principal de un bando), `minor` (aliado menor) o `neutral` |
| `side` | Bando inicial: `axis`, `western_allies`, `ussr` o `neutral` |
| `lateEntry` | No está en juego al empezar. Cuándo entra lo deciden el reglamento y la tabla de refuerzos |
| `offMap` | No tiene territorio en el mapa (entra por los territorios de sus aliados) |

Las colonias y posesiones no son naciones: son territorios del mapa que pertenecen a una nación.

### Fichas (`CounterEntry`, `Counter`)

Las fichas de un mismo tipo son idénticas: tierra (`land`), naval (`naval`) y aérea (`air`). El
catálogo solo dice cuántas tiene cada nación; los factores los pone el reglamento.

`NationCatalog.counters` expande las cantidades en fichas individuales con un id determinista:
código de nación, letra de tipo (`L`, `N`, `A`) y número con al menos dos cifras. Por ejemplo, `DEU-L01`,
`GBR-N04` o `USA-A05`. El orden es fijo (naciones en el orden del catálogo, luego tierra, naval y
aérea), así que los ids son los mismos en todas las plataformas y en cada carga.

### Consultas de `NationCatalog`

- `nation(id)`, `id in catalog`, `nationsOf(side)`.
- `initialSideOf(id)` y `sideOf`, la misma consulta como función `(NationId) -> Side`. Sirve para el
  mapa: `map.territoriesBySide(catalog.sideOf)`.
- `countOf(nation, type)`, `countersOf(nation)`, `counters`, `totalCounters`.

Un `NationCatalog` siempre está validado: solo se crea con `NationCatalog.build(...)` o con
`CatalogJson.decode(...)`, y los dos lanzan `InvalidCatalogException` si algo no cuadra.

## Formato JSON

```json
{
  "formatVersion": 1,
  "id": "test-small",
  "name": "Catálogo de prueba",
  "nations": [
    { "id": "AA", "name": "Alfa", "role": "major", "side": "axis" },
    { "id": "BB", "name": "Bravo", "role": "minor", "side": "western_allies" },
    { "id": "CC", "name": "Charlie", "role": "major", "side": "western_allies", "lateEntry": true, "offMap": true },
    { "id": "DD", "name": "Delta", "role": "neutral", "side": "neutral" }
  ],
  "counters": [
    { "nation": "AA", "type": "land", "count": 3 },
    { "nation": "AA", "type": "air", "count": 1 },
    { "nation": "CC", "type": "naval", "count": 2 }
  ]
}
```

Reglas del formato:

- `formatVersion` es un entero en la raíz y es obligatorio. Hoy solo existe la versión `1`. Un
  catálogo con otra versión, o sin ella, se rechaza antes de leer nada más. Cualquier cambio
  incompatible del formato sube este número.
- Las claves y los valores desconocidos se rechazan.
- Las fichas van en una lista aparte de las naciones, con una entrada por nación y tipo. Una nación
  sin entradas no tiene fichas (en el ejemplo, `BB` y `DD`).
- `lateEntry` y `offMap` valen `false` por defecto y se omiten al escribir.
- `CatalogJson.encode(catálogo)` escribe el JSON compacto, y con `prettyPrint = true` con sangría.
  Leer lo escrito devuelve un catálogo igual, y volver a escribirlo da el mismo texto.

## Validación

`CatalogJson.decode` y `NationCatalog.build` devuelven **todos** los problemas a la vez en
`InvalidCatalogException.issues` (lista de `CatalogIssue`):

| Regla | `CatalogIssue` |
|---|---|
| JSON válido, con `formatVersion` soportada, claves y valores conocidos | `MalformedDocument`, `UnsupportedFormatVersion` |
| Al menos una nación, sin repetidas | `NoNations`, `DuplicateNation` |
| Las fichas son de naciones del catálogo, una entrada por nación y tipo, con cantidad positiva | `UnknownNation`, `DuplicateCounterEntry`, `NonPositiveCount` |
| Una nación neutral empieza en el bando `neutral`, y solo las neutrales pueden | `RoleSideMismatch` |
| Una neutral no tiene entrada tardía | `LateEntryNeutral` |
| Toda nación principal tiene fichas | `MajorWithoutCounters` |

### Catálogo y mapa

El catálogo y el mapa se validan cada uno por su lado. `CatalogMapValidator.validate(catálogo, mapa)`
comprueba que encajan y devuelve una lista de `CatalogMapIssue`:

| Regla | `CatalogMapIssue` |
|---|---|
| Toda nación que no es `offMap` tiene al menos un territorio en el mapa | `NationWithoutTerritory` |
| Toda nación con territorios en el mapa está en el catálogo | `NationNotInCatalog` |
| Una nación `offMap` no tiene territorios | `OffMapNationWithTerritory` |

## Catálogo de Europa, 1939

`data/catalogs/europe-1939.json`: 23 naciones y 152 fichas, con valores propios.

| Bando inicial | Nación | Rol | Tierra | Naval | Aérea |
|---|---|---|---|---|---|
| Eje | Alemania (`DEU`) | principal | 30 | 3 | 6 |
| Eje | Italia (`ITA`) | principal | 11 | 3 | 2 |
| Eje | Finlandia (`FIN`), Rumanía (`ROU`) | menor | 2 cada una | | |
| Eje | Hungría (`HUN`), Bulgaria (`BGR`) | menor | 1 cada una | | |
| Aliados Occidentales | Reino Unido (`GBR`) | principal | 8 | 4 | 4 |
| Aliados Occidentales | Francia (`FRA`) | principal | 6 | 1 | 1 |
| Aliados Occidentales | Estados Unidos (`USA`) | principal, entrada tardía, fuera del mapa | 10 | 3 | 5 |
| Aliados Occidentales | Noruega (`NOR`), Países Bajos (`NLD`), Bélgica (`BEL`), Grecia (`GRC`) | menor | 1 cada una | | |
| Aliados Occidentales | Dinamarca (`DNK`) | menor | sin fichas | | |
| URSS | Unión Soviética (`SUN`) | principal | 26 | 1 | 4 |
| URSS | Polonia (`POL`) | menor | 2 | | |
| URSS | Yugoslavia (`YUG`) | menor | 1 | | |
| Neutral | España (`ESP`), Turquía (`TUR`) | neutral | 3 cada una | | |
| Neutral | Suecia (`SWE`) | neutral | 2 | 1 | |
| Neutral | Suiza (`CHE`) | neutral | 2 | | |
| Neutral | Irlanda (`IRL`), Portugal (`PRT`) | neutral | sin fichas | | |

Por bando: Eje 61, Aliados Occidentales 46, URSS 34 y neutrales 11.

Un test JVM (`core/src/jvmTest/.../Europe1939CatalogTest.kt`) carga el fichero real y comprueba
estos números. La validación cruzada con el mapa real se añadirá cuando exista el mapa de Europa.
