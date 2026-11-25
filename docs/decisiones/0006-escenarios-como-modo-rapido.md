# 0006. Escenarios cortos como modo rápido

- **Estado:** Aceptada
- **Fecha:** 2026-11-25

## Contexto

La campaña completa dura 18 turnos y cubre toda Europa y el norte de África: es una partida de
muchas horas. Hace falta una forma de jugar en una sesión corta, aprender el juego sin afrontar la
campaña entera y tener partidas online que terminen en un tiempo razonable.

## Decisión

- **Modo completo:** la campaña de 18 turnos.
- **Modo rápido:** escenarios cortos de 4 a 6 turnos, centrados en una región y un periodo concretos.
- Un escenario es un dato, no un motor distinto: define estado inicial, región, turnos y condiciones
  de victoria, y se juega con el mismo motor y los mismos reglamentos que la campaña.

## Alternativas consideradas

- **Acelerar la campaña completa** con menos fases o menos unidades: daría un juego distinto con
  reglas propias y duplicaría el trabajo de equilibrio.
- **Solo campaña completa:** deja fuera a quien quiere partidas cortas y hace el online muy lento.

## Consecuencias

- El motor tiene que poder arrancar desde cualquier estado inicial y aceptar condiciones de victoria
  configurables.
- Hace falta un formato de datos para los escenarios, que se documentará cuando se implemente.
- Cada escenario necesita su propio equilibrio y sus pruebas.
- Los escenarios llegan en el hito M3, después del juego completo contra la IA.
