# 0013. Despliegue y refuerzos como dato

- **Estado:** Aceptada
- **Fecha:** 2026-11-26

## Contexto

Cada partida necesita saber qué fichas pone cada nación al empezar, en qué territorios, y qué fichas
recibe en cada turno. El reglamento Avanzado añade además refuerzos especiales opcionales: aliados
menores que entran tarde, voluntarios de un neutral, fichas extra por controlar ciudades clave y la
entrada de un neutral en la guerra. Las cifras se van a reequilibrar muchas veces, el motor no lee
ficheros ([0002](0002-motor-puro-y-determinista.md)) y las diferencias entre reglamentos no pueden
ser `if (classic)` ([0003](0003-reglamento-configurable.md)).

## Decisión

- **Un documento por teatro** (`data/setup/<id>.json`, con `formatVersion`) que reúne el despliegue,
  la tabla de refuerzos y los especiales, porque se equilibran juntos. `core` lo carga con
  `SetupJson.decode(texto, catálogo)` en el paquete `core/.../setup`.
- **Cada ficha del catálogo entra una sola vez.** Despliegue + refuerzos + todas las bonificaciones
  no superan el catálogo de ninguna nación ni tipo. La validación usa el peor caso (todos los
  especiales activos y cumplidos), así que ninguna combinación de opciones pide fichas que no existen.
  Las fichas eliminadas no vuelven como refuerzo.
- **El despliegue referencia territorios por id** (`TerritoryId` del mapa). Se valida contra el mapa
  aparte, con `SetupMapValidator`, como el catálogo con `CatalogMapValidator`.
- **Los especiales son datos con id estable** y un efecto de un conjunto cerrado (aplazamiento,
  voluntarios, bonificación por ciudad, cambio de bando). Sus condiciones son declarativas
  (`ControlsCity(bando, cityId)`, con la ciudad por un id estable, no por su nombre visible, que
  puede cambiar o traducirse).
- **Lo que no decide este modelo**: qué especiales activa cada reglamento (el Clásico ninguno; el
  Avanzado los ofrece como opciones), evaluar las condiciones durante la partida, recordar qué
  bonificaciones se cobraron y aplicar un cambio de bando. Eso es del reglamento (#8), de la
  diplomacia de neutrales (#16) y de la colocación de refuerzos (#15), que leerán estos datos.

El modelo y el formato están en [Despliegue y refuerzos](../formato-despliegue.md) y el razonamiento
de las cifras en [Diseño de la tabla de refuerzos](../diseno-refuerzos.md).

## Alternativas consideradas

- **Ficheros separados para despliegue, refuerzos y especiales:** duplican el id de catálogo y la
  versión, y separan cifras que se reequilibran a la vez.
- **Refuerzos que reciclan fichas eliminadas:** más cercano a un juego de mesa con fichas físicas,
  pero hace imposible comprobar en los datos que la tabla cabe en el catálogo.
- **Condiciones con un lenguaje genérico:** sobra para cuatro reglas. Si hacen falta más, se amplía el
  conjunto cerrado de efectos y condiciones y se sube `formatVersion` si cambia el formato.
- **Especiales dentro del reglamento:** el reglamento decide qué se activa, pero las cifras pertenecen
  al teatro: otro mapa traería otros especiales.

## Consecuencias

- Reequilibrar es editar un JSON; la CI rechaza cualquier cambio que pida fichas que no existen.
- Añadir una bonificación obliga a reservar fichas en la tabla base o a ampliar el catálogo.
- El despliegue real depende de los ids de territorio del mapa de Europa, y los especiales de los ids
  de ciudad. `SetupMapValidator` comprobará esos ids cuando el mapa los tenga.
- El reglamento tendrá que referenciar los especiales por su `SpecialId`.
