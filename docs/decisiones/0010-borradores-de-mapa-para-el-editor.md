# 0010. Borradores de mapa para el editor

- **Estado:** Aceptada
- **Fecha:** 2026-11-26

## Contexto

El mapa completo (unos 42×48 hexes) se digitaliza con el editor del mapa en varias sesiones. Un mapa a
medias casi nunca cumple las reglas: hay tierra sin territorio, territorios todavía vacíos o estrechos
sin terminar. Pero en `core` un `HexMap` solo existe validado
([0009](0009-modelo-del-mapa-hexagonal.md)), y `MapJson.decode` rechaza cualquier mapa que no las
cumpla. Sin otra vía, el editor solo podría guardar mapas válidos, o tendría que escribir el formato
por su cuenta y duplicarlo.

El editor necesita además dónde guardar cómo encaja la rejilla sobre su imagen de referencia, que es
local y no se versiona.

## Decisión

- `core` ofrece `MapDraft`: el contenido de un mapa sin validar, con los tipos del dominio. Tiene
  `issues()` (todos los `MapIssue`), `toMap()` (el `HexMap` validado) y `MapDraft.of(mapa)`.
- `MapJson.decodeDraft` y `MapJson.encode(borrador)` leen y escriben el mismo formato con la misma
  `formatVersion`. `decodeDraft` solo rechaza lo que no es un mapa. `decode` y `encode(mapa)` se
  apoyan en ellas y no cambian de comportamiento.
- **El motor y el juego solo consumen `HexMap`.** `MapDraft` es un formato de edición y de carga, y
  nada de `core` fuera del paquete `map` depende de él.
- La calibración del editor (ruta de la imagen, origen y tamaño del hex en píxeles, opacidades) no
  forma parte del mapa. Va en un fichero aparte, `<mapa>.editor.json`, que git ignora porque contiene
  rutas locales. Es coherente con D1 y D5: la orientación y la escala son del editor, no de `core`.

## Alternativas consideradas

- **Guardar solo mapas válidos:** obligaría a dejar cada sesión de digitalización con un mapa
  coherente, cosa que no es realista.
- **Formato propio del editor o DTO duplicados:** dos formatos que mantener sincronizados.
- **Hacer públicos `MapValidator` y los DTO del JSON:** expone más superficie y ata a los clientes a
  las clases de serialización.
- **Guardar la calibración dentro del JSON del mapa:** mezcla datos con layout y metería rutas
  locales en un fichero versionado.

## Consecuencias

- El editor abre, guarda y valida mapas a medias sin escribir nunca el formato por su cuenta.
- Hay un tipo público más en `core/.../map`. El motor no lo puede usar: si una regla necesita un mapa,
  recibe un `HexMap`.
- La calibración de cada mapa vive solo en la máquina de quien digitaliza. Para continuar en otra
  máquina hay que volver a calibrar o copiar el fichero `.editor.json`.
