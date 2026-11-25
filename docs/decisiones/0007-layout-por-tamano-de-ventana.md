# 0007. Layout por tamaño de ventana

- **Estado:** Aceptada
- **Fecha:** 2026-11-25

## Contexto

La misma interfaz ([0001](0001-kotlin-multiplatform-y-compose.md)) se ve en un móvil en vertical u
horizontal, en una tablet, en una ventana del navegador de cualquier forma y en una ventana de
escritorio redimensionable. La orientación por sí sola no basta para decidir el layout: una tablet en
vertical tiene más espacio que un móvil en horizontal, y en web y escritorio no hay orientación
propiamente dicha.

Hay dos tipos de pantalla con necesidades distintas:

- **Menús** (inicio, nueva partida, opciones): listas y formularios que se leen mal si se estiran a
  todo el ancho.
- **Tablero:** el mapa hexagonal, que aprovecha todo el espacio, más la información de la partida.

## Decisión

- El layout se decide por el **tamaño de la ventana** (*window size class*), no por la orientación.
- **Menús:** siempre en una columna estrecha.
  - En móvil, fijados en vertical.
  - En tablet, web y escritorio, una columna centrada de 480 a 600 px de ancho o dos paneles.
- **Tablero:** funciona en las dos orientaciones.
  - **Horizontal es el diseño principal:** mapa a la mayor parte de la pantalla e información al lado.
  - **Vertical es la alternativa:** mapa arriba y panel de información abajo.
  - En web y escritorio se adapta a la forma de la ventana.

## Alternativas consideradas

- **Layout por orientación:** falla en tablets, en web y en escritorio, donde la orientación no
  indica el espacio disponible.
- **Todo fijado en vertical en móvil:** el tablero hexagonal se juega mucho mejor en horizontal.
- **Tablero solo en horizontal:** obliga a girar el móvil para jugar una partida rápida.
- **Menús a todo el ancho:** en pantallas grandes quedan líneas larguísimas y controles dispersos.

## Consecuencias

- Las pantallas reciben el tamaño de ventana y eligen su layout a partir de él. No se consulta la
  plataforma ni la orientación.
- El tablero necesita dos disposiciones (panel lateral y panel inferior) que se mantienen y se prueban
  las dos.
- En móvil, la app tiene que bloquear la rotación en los menús y permitirla en el tablero.
- El mismo código sirve para ventanas de cualquier forma en web y escritorio.
