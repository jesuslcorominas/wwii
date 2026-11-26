# 0005. Online asíncrono con plazo por turno

- **Estado:** Aceptada
- **Fecha:** 2026-11-25

## Contexto

Una campaña completa es larga y los bandos juegan por turnos, así que es poco realista exigir que
todos los jugadores estén conectados a la vez durante toda la partida. Sin backend propio
([0004](0004-sin-backend-propio.md)) tampoco hay un servidor que gestione salas en tiempo real.
Además, un jugador que abandona no debe bloquear la partida de los demás.

El orden de prioridades del producto es:

1. Un jugador contra la IA y hotseat local.
2. Online.

## Decisión

- El multijugador online es **asíncrono**: cada bando juega su turno cuando puede y la partida avanza
  cuando el turno se completa.
- Cada partida tiene un **plazo por turno configurable**: minutos, para jugar en vivo, o 24 o 48
  horas, para jugar en diferido.
- **Si vence el plazo, ese bando pierde el turno, no la partida.**
- Modos online:
  - **2 y 3 jugadores:** la IA controla los bandos que nadie ocupa. Se entra por código de partida o
    por emparejamiento aleatorio.
  - **4 jugadores:** solo humanos y solo por código. Los Aliados Occidentales se reparten entre dos
    jugadores: uno lleva las fuerzas de tierra y otro las de aire y mar.
- Lo hacen cumplir las reglas de seguridad de Firestore y la validación por replay en cada cliente
  ([0002](0002-motor-puro-y-determinista.md)).

## Alternativas consideradas

- **Online en tiempo real:** exige que todos estén conectados a la vez y un servidor que mantenga las
  salas.
- **Sin plazo:** un jugador inactivo bloquearía la partida indefinidamente.
- **Perder la partida al vencer el plazo:** castiga demasiado un despiste en partidas que duran días.
- **Partidas de 4 con emparejamiento aleatorio:** coordinar a cuatro desconocidos en una partida
  larga es poco realista, y el reparto de los Occidentales necesita acuerdo entre dos jugadores.

## Consecuencias

- Las partidas largas se pueden jugar a ratos, y las cortas en vivo con un plazo de minutos.
- La IA tiene que poder ocupar cualquier bando a mitad de partida en los modos de 2 y 3.
- El modelo de turno tiene que contemplar un turno perdido por plazo y el reparto de los Occidentales
  entre dos jugadores.
- Hacen falta notificaciones o algún aviso de que te toca jugar.
- El online llega después del juego local (hito M5), sobre el mismo motor.
