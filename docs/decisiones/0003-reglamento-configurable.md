# 0003. Reglamento configurable: Clásico y Avanzado

- **Estado:** Aceptada
- **Fecha:** 2026-11-25

## Contexto

Cada partida se juega con uno de dos reglamentos:

- **Clásico:** más sencillo y rápido de aprender.
- **Avanzado:** añade reglas y matices a las del Clásico.

Comparten la mayor parte de las reglas y difieren en puntos concretos. Si cada diferencia se
resolviera con condiciones repartidas por el motor, añadir o cambiar una regla obligaría a buscar
todos los sitios donde aparece, y sería fácil que un reglamento heredara por error una regla del otro.

## Decisión

- El reglamento es un **objeto de configuración y estrategia** que recibe el motor al crear la
  partida y que forma parte del estado inicial.
- Cada regla que difiere entre reglamentos se modela como una opción o una estrategia de ese objeto,
  no con `if (classic)` repartidos por el código.
- "Clásico" y "Avanzado" son dos configuraciones predefinidas. El modelo permite activar opciones
  sueltas si más adelante se quiere ofrecer.

## Alternativas consideradas

- **Condiciones `if (classic)` en el motor:** rápido al principio, pero la diferencia entre
  reglamentos queda repartida y no se puede probar cada reglamento por separado.
- **Dos motores independientes:** duplica todo lo común y obliga a corregir cada error dos veces.

## Consecuencias

- Las diferencias entre reglamentos están en un solo sitio y se pueden leer y revisar juntas.
- Cada reglamento se prueba con su propia configuración.
- Como el reglamento forma parte del estado inicial, el replay ([0002](0002-motor-puro-y-determinista.md))
  reproduce la partida con las mismas reglas con que se jugó.
- Diseñar cada regla nueva exige decidir si es común o depende del reglamento, y en el segundo caso
  darle su punto de extensión.
