# Editor del mapa

Herramienta interna de escritorio para crear y editar el mapa sin escribir JSON a mano. Superpone la
rejilla hexagonal sobre una imagen de referencia y permite pintar el modelo de
[Mapa: modelo y formato JSON](formato-mapa.md). No se distribuye: vive en el módulo `mapeditor` y solo
depende de `core`.

```bash
./gradlew :mapeditor:run
./gradlew :mapeditor:run --args="<ruta al mapa>.json"
```

## Imagen de referencia

**Archivo › Cargar imagen de referencia** abre una imagen PNG o JPG desde cualquier ruta local. La
imagen no se copia ni se versiona: el editor solo guarda su ruta en el fichero de calibración. No
metas en el repo imágenes ni material de terceros.

## Calibrar la rejilla

La rejilla es pointy-top (D5) y se mide en píxeles de la imagen:

| Campo | Qué es |
|---|---|
| Origen X, Y | Centro del hex `(0, 0)` |
| Ancho | Distancia entre los centros de dos hexes vecinos de la misma fila (de lado plano a lado plano) |
| Alto | De punta a punta del hex |

Los campos se aplican al pulsar Enter o al salir de ellos, y el tamaño mínimo es de 4 píxeles. Un hex
regular tiene `alto = ancho × 2 / √3`. Ancho y alto van por separado porque un escaneo puede
estar algo estirado; con **Mantener proporción** cambian juntos. La herramienta **Mover rejilla**
arrastra el origen sobre la imagen. El punto dorado marca el centro del hex `(0, 0)`.

Para calibrar, haz coincidir primero un hex de una esquina, ajusta el ancho hasta que la fila encaje
a lo largo de toda la imagen, y luego el alto con las filas.

Las opacidades de la imagen y de los datos ayudan a ver el original debajo de lo pintado.

La calibración se guarda junto al mapa en `<mapa>.editor.json` (por ejemplo, `europa.json` →
`europa.editor.json`). Contiene rutas locales, así que git la ignora. Ver
[0010](decisiones/0010-borradores-de-mapa-para-el-editor.md).

## Navegar

- Rueda del ratón: zoom sobre el cursor.
- Botón derecho o central y arrastrar: desplazar.
- **Vista › Encajar**: la imagen entera en la ventana.

La rejilla solo se dibuja en la parte visible y cuando los hexes miden al menos 8 píxeles en pantalla;
con menos zoom solo se ven los hexes pintados.

## Pintar

Elige una herramienta en el panel lateral y haz clic o arrastra sobre los hexes (un arrastre rápido
pinta también los hexes intermedios). Cada trazo, cada estrecho, cada ciudad y cada cambio de id o
nombre se deshace de una vez (**Edición › Deshacer** y **Rehacer**).

| Herramienta | Efecto |
|---|---|
| Tierra, Mar, Lago | Pone el hex en el tablero con ese terreno. Al pasar a mar o lago pierde territorio, ciudad, montaña, aeropuerto y los ríos de sus lados |
| Quitar hex | Lo saca del tablero (con sus ríos) |
| Territorio | El hex pasa a ser tierra del territorio elegido en el panel |
| Montaña, Aeropuerto | Pone o quita el rasgo en hexes de tierra |
| Ciudad | Abre un diálogo para el nombre y el tipo (ciudad, capital de territorio o de nación), o para quitarla |
| Río | Pone o quita el río en el lado más cercano al cursor |
| Estrecho | Clic en un hex de mar y después en sus dos orillas. Clic en un estrecho existente lo quita |
| Mover rejilla | Arrastra el origen de la rejilla |
| Inspeccionar | No cambia nada; el panel muestra el hex bajo el cursor |

Los **territorios** se crean, editan y borran en su panel: id, nación (por ejemplo `FR`), nombre,
tipo (país, colonia o posesión) y si es discontiguo. El color de cada territorio sale de su nación
(las colonias y posesiones, más pálidas). Es solo una ayuda para editar: el juego colorea por bando.
Cambiar el id de un territorio mueve sus hexes; borrarlo los deja como tierra sin territorio.

Las operaciones solo arreglan lo que les toca. Si algo deja de cuadrar (un estrecho cuya orilla pasa
a ser mar, por ejemplo), lo señala el validador.

## Problemas del validador

El panel **Problemas del validador** lista todos los `MapIssue` del mapa y se recalcula con cada
cambio. Los hexes afectados se marcan en rojo; al hacer clic en un problema, la vista se centra en sus
hexes y los resalta.

## Abrir y guardar

- **Abrir** lee el JSON del formato del mapa. Un mapa que rompe las reglas se abre y muestra sus
  problemas; un fichero que no es un mapa (JSON roto, `formatVersion` desconocida, claves mal
  escritas) se rechaza con el motivo en la barra de estado.
- **Guardar** escribe el JSON con `MapJson` (con sangría y con su `formatVersion`) y el fichero de
  calibración. Un mapa con problemas también se guarda, con un aviso: la digitalización se hace en
  varias sesiones.
- Abrir y guardar sin tocar nada deja el fichero igual si ya estaba escrito por el editor (con
  sangría y sin valores por defecto explícitos). Un JSON escrito a mano o compacto se reescribe con ese
  formato, con los mismos datos. El editor conserva el orden de hexes, territorios y ríos, y añade al
  final lo nuevo, así que los diffs solo muestran lo que se ha cambiado.
- Si el mapa no se puede leer o guardar (fichero inexistente, sin permisos, disco lleno), el motivo
  sale en la barra de estado y los cambios siguen en el editor.
- Si hay cambios sin guardar, el editor pregunta antes de cerrar, abrir otro mapa o empezar uno nuevo.

## Código

- `model/`: lógica pura y testeada. `HexLayout` (hex ↔ píxel), operaciones de edición sobre `MapDraft`
  (`MapEdits.kt`), `History`, `Calibration`, `MapFiles`, `NationColors` e `IssueLocator`.
- `ui/`: Compose Desktop. `EditorState`, `MapCanvas`, `SidePanel` y los diálogos.
- Tests: `./gradlew :mapeditor:test`.
