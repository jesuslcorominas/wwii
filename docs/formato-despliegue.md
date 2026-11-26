# Despliegue y refuerzos: modelo y formato JSON

Qué fichas pone cada nación al empezar la partida y en qué territorios, qué fichas recibe en cada
turno y qué refuerzos especiales ofrecen las reglas opcionales. El código está en
`core/src/commonMain/kotlin/com/jesuslcorominas/wwii/core/setup/` y los datos de la campaña de Europa
en [`data/setup/europe-1939.json`](../data/setup/europe-1939.json). El porqué de cada cifra está en
[Diseño de la tabla de refuerzos y del despliegue inicial](diseno-refuerzos.md), y las decisiones de
fondo en [0013. Despliegue y refuerzos como dato](decisiones/0013-despliegue-y-refuerzos-como-dato.md).

## Qué hace `core` y qué no

| `core/.../setup` | Fuera de este paquete |
|---|---|
| Modelo, validación contra el catálogo y `SetupJson.decode(texto, catálogo)` / `SetupJson.encode(setup)` | Leer el fichero (`ui`, tests JVM) |
| Validación cruzada con un mapa (`SetupMapValidator`) | Colocar los refuerzos en la partida y perder los que no caben |
| Especiales como datos con id estable | Qué especiales activa cada reglamento, evaluar sus condiciones, cambiar de bando |

## Modelo

| Tipo | Qué es |
|---|---|
| `CounterCounts(land, naval, air)` | Cantidad de fichas de cada tipo |
| `Deployment(nation, territory, counts)` | Fichas de una nación en un territorio al empezar |
| `Reinforcement(turn, nation, counts)` | Fichas que recibe una nación al principio de un turno (1 a `turns`) |
| `SpecialReinforcement(id, name, effect)` | Regla opcional con un `SpecialId` estable |
| `GameSetup` | Todo lo anterior para un catálogo y un número de turnos. Solo existe validado |

Efectos de los especiales (`SpecialEffect`):

| Efecto | Significado | ¿Añade fichas? |
|---|---|---|
| `DeferredEntry(nations)` | Cada nación de la lista no despliega: su despliegue entero llega como refuerzo en el turno indicado | No |
| `Volunteers(nation, side, turn, counts)` | `counts` se retiran del despliegue de la nación y llegan en `turn` luchando por `side`; la nación conserva su bando | No |
| `KeyCityBonus(bonuses)` | Cada `CityBonus(condition, nation, counts)` da `counts` una sola vez, la primera vez que se cumple la condición | Sí |
| `JoinSide(nation, side, condition)` | La nación pasa a `side`, con lo que tenga, cuando se cumple la condición | No |

La única condición por ahora es `ControlsCity(side, cityId)`: el bando controla la ciudad con ese id
estable (un slug como `suez` o `gibraltar`, el id de la ciudad en el mapa).

Consultas de `GameSetup`: `deploymentOf(nación)`, `reinforcementsAt(turno)`,
`reinforcementsOf(nación)`, `deployedCountOf`, `reinforcementCountOf`, `baseTotalOf` (despliegue más
refuerzos base) y `special(id)`.

## Formato JSON

```json
{
  "formatVersion": 1,
  "id": "europe-1939",
  "name": "Campaña de Europa 1939-1945",
  "catalog": "europe-1939",
  "turns": 18,
  "deployment": [
    { "nation": "DEU", "territory": "DEU", "land": 12, "naval": 2, "air": 3 }
  ],
  "reinforcements": [
    { "turn": 2, "nation": "DEU", "land": 2 }
  ],
  "specials": [
    { "type": "deferred_entry", "id": "axis-minors-late-entry", "name": "…",
      "nations": [ { "nation": "BGR", "turn": 5 }, { "nation": "ROU", "turn": 6 } ] },
    { "type": "volunteers", "id": "spanish-volunteers", "name": "…",
      "nation": "ESP", "side": "axis", "turn": 7, "land": 1 },
    { "type": "key_city_bonus", "id": "key-cities", "name": "…",
      "bonuses": [ { "cityId": "suez", "side": "axis", "nation": "DEU", "land": 1 } ] },
    { "type": "join_side", "id": "spain-joins-axis", "name": "…",
      "nation": "ESP", "side": "axis",
      "condition": { "type": "controls_city", "side": "axis", "cityId": "gibraltar" } }
  ]
}
```

- `formatVersion`: hoy solo existe la 1. Cualquier otra se rechaza.
- `catalog`: id del catálogo para el que están escritos los datos. Tiene que coincidir con el que se
  pasa a `decode`.
- `land`, `naval` y `air` valen 0 si se omiten, y `encode` no escribe los ceros.
- Los especiales y las condiciones llevan su tipo en la clave `type`.
- Bandos en minúsculas: `axis`, `western_allies`, `ussr`, `neutral`.
- Las claves desconocidas se rechazan.

## Validación contra el catálogo

`GameSetup.build` y `SetupJson.decode` devuelven todos los problemas a la vez en
`InvalidSetupException.issues` (`SetupIssue`):

| Problema | Cuándo |
|---|---|
| `UnsupportedFormatVersion`, `MalformedDocument` | Versión no soportada, JSON mal formado, clave o tipo desconocido, id en blanco |
| `CatalogMismatch` | `catalog` no es el id del catálogo recibido |
| `NonPositiveTurns`, `TurnOutOfRange` | Campaña sin turnos, o turno fuera de `1..turns` |
| `UnknownNation` | Nación que no está en el catálogo |
| `InvalidCounts` | Cantidad negativa o entrada vacía |
| `DuplicateDeployment`, `DuplicateReinforcement`, `DuplicateSpecial` | Misma nación y territorio, mismo turno y nación, o mismo id dos veces |
| `NotDeployable` | Despliegue de una nación de entrada tardía o fuera del mapa |
| `NeutralReinforcement` | Refuerzo base para una nación neutral |
| `ExceedsCatalog` | Despliegue + refuerzos + todas las bonificaciones superan las fichas del catálogo de un tipo |
| `EmptySpecial` | Aplazamiento sin naciones o bonificación sin ciudades |
| `DeferredWithoutDeployment` | Se aplaza una nación que no despliega nada |
| `WithheldExceedsDeployment` | Los voluntarios retiran más fichas de las que la nación despliega |
| `InvalidSide` | Voluntarios o cambio de bando hacia `neutral` o hacia el bando que ya tiene la nación; bonificación para una nación que no es del bando de la condición |

## Validación contra el mapa

`SetupMapValidator.validate(setup, catálogo, mapa)` devuelve una lista de `SetupMapIssue`:

| Problema | Cuándo |
|---|---|
| `UnknownTerritory` | El territorio del despliegue no está en el mapa |
| `TerritoryOfAnotherSide` | El territorio es de una nación de otro bando inicial |
| `NoCoast` | Hay fichas navales y el territorio no tiene costa |
| `NoAirport` | Hay fichas aéreas y el territorio no tiene aeropuerto ni ciudad |

Desplegar en territorio de un aliado del mismo bando es válido (por ejemplo, tropas británicas en
Francia en 1939).

Las ciudades de los especiales todavía no se comprueban contra el mapa: el modelo del mapa aún no
tiene un id estable de ciudad. La comprobación se añade aquí cuando lo tenga.
