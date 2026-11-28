# Mapa de Europa y norte de África, 1939

El tablero del juego está en [`data/maps/europe-1939.json`](../data/maps/europe-1939.json), con el
formato de [Mapa: modelo y formato JSON](formato-mapa.md). No está dibujado a mano: lo escribe un
generador versionado en [`tools/mapgen/`](../tools/mapgen/) a partir de geografía de dominio
público y de datos de diseño propios. Se puede regenerar idéntico y retocar después en el editor
del mapa. Las decisiones de fondo están en
[0012. Mapa generado desde geografía de dominio público](decisiones/0012-mapa-generado-desde-geografia-publica.md).

## Qué contiene

| | |
|---|---|
| Rejilla | Hex en punta de 110 km, 42 columnas × 49 filas sobre la proyección cónica conforme de Lambert (paralelos 38° y 62°, origen 50° N y 18° E) |
| Hexes | 1939: 1140 de tierra, 797 de mar y 2 de lago. La tierra fuera del juego queda fuera del tablero |
| Territorios | 39: 22 países y 17 colonias y posesiones, con las fronteras de septiembre de 1939 |
| Ciudades | 278, cada una con su id estable; 22 capitales de nación, 17 capitales de territorio y las 36 ciudades objetivo |
| Aeropuertos sin ciudad | 2 (Banak y Sidi Barrani); toda ciudad es además aeropuerto |
| Montañas | 151 hexes |
| Ríos | 371 lados de hex |
| Estrechos | 8: Gibraltar, Dardanelos, Bósforo, Mesina, Kerch, Dover, Øresund y Bonifacio |

### Territorios

Los países usan como id el código de su nación (ISO 3166 alfa-3, `SUN` para la URSS). Las colonias y
posesiones usan `<NACIÓN>-<código>`. Estos ids los usan otros datos (por ejemplo, el despliegue
inicial), así que no cambian; un test los fija.

| Nación | Territorios |
|---|---|
| Alemania | `DEU` (con Austria, Bohemia-Moravia, Memel, Danzig y Prusia Oriental) · `DEU-SVK` Eslovaquia |
| Italia | `ITA` (con Istria y Fiume) · `ITA-LBY` Libia · `ITA-ALB` Albania · `ITA-DOD` Dodecaneso |
| Reino Unido | `GBR` (con Irlanda del Norte) · `GBR-EGY` Egipto · `GBR-PSE` Palestina · `GBR-JOR` Transjordania · `GBR-CYP` Chipre · `GBR-MLT` Malta · `GBR-GIB` Gibraltar |
| Francia | `FRA` (con Córcega) · `FRA-DZA` Argelia · `FRA-TUN` Túnez · `FRA-MAR` Marruecos francés · `FRA-SYR` Siria y Líbano |
| URSS | `SUN` · `SUN-BAL` Países Bálticos |
| España | `ESP` · `ESP-MAR` Marruecos español |
| Dinamarca | `DNK` · `DNK-ISL` Islandia |
| Resto | `FIN`, `ROU`, `HUN`, `BGR`, `NOR`, `NLD`, `BEL` (con Luxemburgo), `GRC`, `POL`, `YUG`, `TUR`, `SWE`, `CHE`, `IRL`, `PRT` |

Fronteras de 1939 que no coinciden con las actuales: Polonia de 1921-1939 (con Wilno y Lwów),
Rumanía con Besarabia, Bucovina del Norte y Dobruja del Sur, Hungría con el sur de Eslovaquia y la
Rutenia subcarpática, Finlandia con Carelia y Petsamo, y Turquía con Hatay.

### Ciudades objetivo

El mapa contiene las 36 ciudades objetivo, pero no las marca: qué ciudades cuentan para la victoria
y cómo lo decide el reglamento, que las cita por su id. Son `london`, `paris`, `moscow`, `berlin`,
`rome`, `stalingrad`, `leningrad`, `suez`, `tripoli`, `helsinki`, `gibraltar`, `kiev`, `valletta`,
`copenhagen`, `oslo`, `athens`, `sofia`, `bucharest`, `budapest`, `belgrade`, `warsaw`, `brussels`,
`rotterdam`, `algiers`, `casablanca`, `madrid`, `barcelona`, `dublin`, `ankara`, `istanbul`, `bern`,
`stockholm`, `amman`, `tunis`, `kharkov` y `kursk`.

Los ids de ciudad son el exónimo inglés en minúsculas ASCII cuando hay uno de uso común (`rome`,
`kharkov`); si no, el nombre local sin acentos (`lodz`, `brest-litovsk`). El nombre visible está en
español y puede cambiar sin tocar el id.

## Cómo se genera

```
Natural Earth 5.1.2  ──┐
(tierra, países,       │
 lagos, ríos,          ├─>  tools/mapgen/mapgen.py  ──>  data/maps/europe-1939.json
 cordilleras)          │
tools/mapgen/          │
 europe1939.py  ───────┘
```

- `mapgen.py` es el motor. Proyecta la geografía, recorre la rejilla y muestrea unos 85 puntos por
  hex. Clasifica cada hex y su territorio, coloca ciudades, traza los ríos por los lados de hex y
  escribe el JSON.
- `europe1939.py` son los datos de diseño del teatro: territorios, qué país actual corresponde a
  cada territorio, polígonos de las fronteras de 1939 en longitud y latitud, cortes del Sáhara,
  ciudades con sus coordenadas e ids, estrechos, cordilleras, ríos, lagos y retoques.

Reglas del generador:

- **Terreno.** Un hex es de mar si el mar ocupa al menos la mitad; si no, es de tierra, salvo que
  los lagos elegidos ocupen más que la tierra, y entonces es lago («lo que ocupe más superficie»). Las islas de entre 1000 y
  50 000 km² conservan al menos un hex, y todo hex que cubran en un 35 % o más.
- **Territorio.** El que más puntos de tierra del hex tienen. Cada punto mira primero los polígonos
  de 1939 y luego el país actual. La tierra de Irak, Irán, Arabia y el Sáhara al sur de los cortes
  queda fuera del tablero, y también el mar que no comunica con el océano.
- **Montaña.** Si las cordilleras elegidas cubren al menos la mitad de la tierra del hex.
- **Ríos.** Cada línea de río se ajusta al vértice de hex más cercano y se recorre por los lados.
  Solo cuentan los lados entre dos hexes de tierra.
- **Ciudades.** En el hex que contiene sus coordenadas. Si ese hex es de mar, de otro territorio o ya
  tiene ciudad, la ciudad pasa al hex libre más cercano de su territorio, a 135 km como mucho.
  Primero se colocan las capitales y luego las ciudades objetivo.
- **Estrechos.** Se eligen a mano en coordenadas axiales, mirando la rejilla generada: el hex de mar
  y sus dos orillas de tierra.
- **Retoques.** `FORCED_HEXES` (por longitud y latitud) y `HEX_OVERRIDES` (por `(q, r)`) cambian
  hexes concretos: Gibraltar, Malta, Rodas, el Marruecos español, la Tracia turca, la península de
  Kerch, Flandes, Haifa y la isla de Kolguev.
- Las coordenadas siguen el editor: la fila `j` es `r = j` (hacia el sur) y la columna `i` es
  `q = i - (j - (j & 1)) / 2`; las filas impares van desplazadas medio hex a la derecha.
- La salida es determinista y canónica: el mismo texto que escribe `:mapeditor` al guardar.

### Regenerar

1. Descarga los cinco ficheros de Natural Earth 5.1.2 en GeoJSON, de la etiqueta `v5.1.2` del
   repositorio `nvkelso/natural-earth-vector` (carpeta `geojson/`), a un directorio fuera del repo:

   ```bash
   mkdir -p /tmp/natural-earth
   ```

   ```bash
   for f in ne_50m_land ne_50m_admin_0_countries ne_50m_lakes ne_10m_geography_regions_polys ne_10m_rivers_lake_centerlines; do curl -sfL -o /tmp/natural-earth/$f.geojson https://raw.githubusercontent.com/nvkelso/natural-earth-vector/v5.1.2/geojson/$f.geojson; done
   ```

2. Comprueba que son los ficheros de esta versión:

   | Fichero | SHA-256 |
   |---|---|
   | `ne_50m_land.geojson` | `e874b27a51d146452be360cafb3cc50c86001074a67d534113e6534682f9826b` |
   | `ne_50m_admin_0_countries.geojson` | `3e458fc036ad0a66411f2c1e6cac49c5d7bfb81cb1123bc513b22511a2b7fdeb` |
   | `ne_50m_lakes.geojson` | `d350b75978b26fe839b797c2c529b2fb8f47fb3983c03f4964e36d5df9378a52` |
   | `ne_10m_geography_regions_polys.geojson` | `b7b26e50ea917d3696aec87f932def2bf5f890f5770e441d59c162c6f4c92a77` |
   | `ne_10m_rivers_lake_centerlines.geojson` | `bb854a900ecbd3b408df46d5e16e3e0f974ba55993f9d8b5c26e855273c0905a` |

   ```bash
   shasum -a 256 /tmp/natural-earth/*.geojson
   ```

3. Genera el mapa desde la raíz del repo. Solo necesita Python 3 con la biblioteca estándar:

   ```bash
   python3 -I tools/mapgen/mapgen.py /tmp/natural-earth data/maps/europe-1939.json
   ```

   El generador avisa de las ciudades que ha movido. Si algo no cuadra (una ciudad sin hex libre,
   una ciudad objetivo que falta, una orilla de estrecho que no es tierra), lo dice y no escribe el
   fichero.

4. Comprueba el resultado:

   ```bash
   ./gradlew :core:jvmTest
   ```

   Si los datos de diseño no han cambiado, el fichero sale idéntico y `git diff` no muestra nada.

### Revisarlo en el editor

Con `--reference <png>` el generador dibuja además la geografía real en una imagen PNG: costas,
lagos, ríos, fronteras actuales en gris claro y las fronteras de 1939 del mapa en rojo, por los
lados de hex. Los polígonos de `BORDERS_1939` no se dibujan: son toscos a propósito y solo sirven
para asignar hexes. La calibración va en `data/maps/europe-1939.editor.json`, que git
ignora. Necesita Pillow. Ni la imagen ni la calibración se versionan.

```bash
python3 -I tools/mapgen/mapgen.py /tmp/natural-earth data/maps/europe-1939.json --reference /tmp/europe-1939.png
```

```bash
./gradlew :mapeditor:run --args="data/maps/europe-1939.json"
```

### Retoques

Un retoque hecho en el editor se pierde al regenerar. Para que dure, pásalo a `europe1939.py`:
a `HEX_OVERRIDES` si es un hex, o a `CITIES`, `STRAITS`, `EXTRA_RIVER_SIDES` o
`REMOVED_RIVER_SIDES`. Luego regenera. El editor sirve para ver y probar; el generador es la fuente.

## Decisiones de diseño

- **42 × 49 hexes**, una fila más que los 42 × 48 de D5. Con 48, el golfo de Sirte se salía del
  tablero y Libia quedaba partida en dos.
- **Un territorio de país por nación**, con las colonias y posesiones aparte. Ni la URSS ni Francia se
  trocean: las reglas conquistan por país.
- **Estados bálticos**: posesión soviética (`SUN-BAL`). El juego empieza con los pactos de asistencia
  de otoño de 1939 y así el catálogo no cambia.
- **Eslovaquia**: posesión alemana (`DEU-SVK`), estado cliente del Eje desde marzo de 1939. Bohemia
  y Moravia van dentro de Alemania.
- **Islandia**: posesión danesa (`DNK-ISL`).
- **Luxemburgo**, Andorra, Liechtenstein, Mónaco, San Marino y el Vaticano no tienen hex propio a
  110 km: los absorbe el país vecino (Luxemburgo, Bélgica). Tánger va con el Marruecos español y
  Danzig con Alemania.
- **Egipto** es una posesión británica: en 1939 era nominalmente independiente, pero estaba bajo
  ocupación militar del Reino Unido.
- **Estrechos**: los seis de la issue, con el Bósforo separado de los Dardanelos, más Bonifacio. El
  Gran Belt no aparece: a esta escala Fionia no tiene hex propio, así que Jutlandia y Selandia son
  hexes vecinos por tierra y no hay dos orillas que separar.
- **Gibraltar** es un hex británico en la orilla norte del estrecho; la orilla sur es el Marruecos
  español. Gibraltar no toca la costa africana por tierra.
- **Países pequeños**: los Países Bajos tienen 2 hexes, Bélgica 3, Dinamarca 4 y Suiza 3. Las
  ciudades que no caben en hex propio se han quitado de la lista (Groninga, Arnhem, Aalborg, Calais,
  Dover…).
- **Ciudades costeras** cuyo hex es de mar pasan al hex de tierra más cercano de su territorio (por
  ejemplo, Copenhague a Selandia, Malmö al norte de Escania, Estambul a la orilla europea del
  Bósforo).

## Fuentes y licencias

- **[Natural Earth](https://www.naturalearthdata.com/)** 5.1.2: costa, países actuales, lagos,
  ríos y cordilleras. Es de dominio público ([condiciones de uso](https://www.naturalearthdata.com/about/terms-of-use/)):
  se puede usar sin permiso ni atribución, aunque se agradece citarla. No se versiona en el repo.
- **Fronteras de 1939, ciudades, estrechos y retoques**: datos propios en `europe1939.py`, a partir de
  conocimiento histórico general. Las coordenadas de las ciudades son hechos geográficos.
- **Nada del tablero de NAC.** Ni su rejilla, ni el trazado de sus territorios, ni la posición de sus
  ciudades. Del reglamento original solo se toman conceptos de juego, como que existen 36 ciudades
  objetivo y cuáles son.

## Límites conocidos

- Los ríos se ajustan a los lados de hex de forma automática. En algún tramo rodean un hex en vez de
  cruzarlo, y quedan trozos sueltos donde el río corre por la costa. Se corrigen con
  `EXTRA_RIVER_SIDES` y `REMOVED_RIVER_SIDES`.
- Las cordilleras de Natural Earth son zonas aproximadas para etiquetas, no curvas de nivel.
- Los polígonos de 1939 tienen pocos vértices: a 110 km por hex no hace falta más precisión, pero no
  sirven para dibujar fronteras finas.
