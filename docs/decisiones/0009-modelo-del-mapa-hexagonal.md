# 0009. Modelo del mapa hexagonal

- **Estado:** Aceptada
- **Fecha:** 2026-11-26

## Contexto

Todo el motor (movimiento, zonas de control, combate, transporte, conquista, refuerzos, victoria)
consulta el mapa, y lo usan también el editor del mapa y el catálogo de naciones. El mapa tiene que ser
datos, independiente de cómo se dibuje, y dar el mismo resultado en todas las plataformas
([0002](0002-motor-puro-y-determinista.md)).

## Decisión

- **Coordenadas axiales** `(q, r)`, con la cúbica `s` derivada. `core` no conoce la orientación
  (plano o en punta) ni los píxeles: eso es de `ui` y del editor. Las direcciones son índices `0..5`
  y los lados del JSON son pares de hexes.
- **Mapa disperso:** lista de hexes; lo que no está, está fuera del tablero.
- **Costa deducida** del terreno (tierra con un vecino de mar), no guardada.
- **Ríos por lado** de hex, entre dos hexes de tierra.
- **Estrecho como hex de mar** que declara sus dos orillas de tierra. El cruce terrestre lo decide el
  reglamento.
- **El bando no está en el mapa.** El mapa agrupa territorios por nación y recibe una función
  nación → bando para agrupar por bando. Los tipos compartidos `NationId` y `Side` viven en
  `core/.../nation/`, para que el catálogo de naciones los use sin depender del mapa.
- **JSON separado del modelo:** clases de serialización propias, `formatVersion` obligatorio, claves
  desconocidas rechazadas y validación que informa de todos los problemas a la vez.

El detalle está en [Mapa: modelo y formato JSON](../formato-mapa.md).

## Alternativas consideradas

- **Coordenadas offset (fila y columna):** dependen de la orientación y complican distancias y
  líneas.
- **Direcciones con nombre de brújula o lados como hex más dirección:** dependen de la orientación.
- **Costa como campo del mapa:** duplica un dato deducible y puede contradecir el terreno.
- **Estrecho como lado entre dos hexes de tierra:** los mares de cada lado quedarían a distancia 2 y
  los barcos no podrían cruzarlo; no habría sitio para el barco que hace de puente en el reglamento
  Avanzado, y la zona de control de la orilla no cerraría el paso.
- **Bando dentro del mapa:** el bando depende del reglamento y cambia durante la partida.
- **Serializar directamente el modelo:** ata el formato del fichero a las clases internas.

## Consecuencias

- `ui` y el editor eligen orientación y tamaño del hex sin tocar `core` ni los datos.
- Un `HexMap` siempre está validado; el motor no tiene que comprobar el mapa en cada regla.
- El catálogo de naciones aporta el bando sin cambiar el mapa.
- Las reglas de movimiento y transporte tienen que tratar los estrechos de forma explícita.
- Cualquier cambio incompatible del formato obliga a subir `formatVersion` y a migrar los mapas
  guardados.
