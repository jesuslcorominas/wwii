# 0004. Sin backend propio: Firebase en plan gratuito detrás de interfaces

- **Estado:** Aceptada
- **Fecha:** 2026-11-25

## Contexto

El juego necesitará servicios remotos para el multijugador online, la identidad de los jugadores, la
configuración remota y el registro de errores. Es un proyecto personal y el objetivo es que el coste
de operación sea cero. Además, el juego tiene que funcionar entero sin red: contra la IA y en hotseat
local.

## Decisión

- **No hay backend propio**: ni servidor ni funciones en la nube que mantener.
- Los servicios remotos los da **Firebase en su plan gratuito**: Auth, Firestore, Hosting, Remote
  Config y Crashlytics.
- Firebase solo aparece en la **capa de datos**, detrás de interfaces propias (por ejemplo,
  `GameRepository`), con una implementación por plataforma.
- `core` no conoce Firebase. El juego local no depende de la red.
- Sin servidor que arbitre, la legalidad de las partidas online la garantizan las reglas de seguridad
  de Firestore y la validación por replay en cada cliente ([0002](0002-motor-puro-y-determinista.md)).

## Alternativas consideradas

- **Servidor propio que valide cada jugada:** es lo más robusto contra trampas, pero tiene coste
  fijo, mantenimiento y operación.
- **Cloud Functions de Firebase:** requieren el plan de pago.
- **Usar el SDK de Firebase directamente en el código compartido:** acoplaría el juego a un
  proveedor, complicaría los tests y no hay SDK oficial que cubra todos los targets por igual.

## Consecuencias

- Coste de operación cero mientras el uso quepa en las cuotas gratuitas.
- El juego funciona y se prueba sin red. En los tests, la capa de datos se sustituye por
  implementaciones falsas escritas a mano.
- Cambiar de proveedor solo afecta a las implementaciones de las interfaces.
- Las reglas de seguridad de Firestore pasan a ser código crítico, con su propio flujo de despliegue
  cuando se integre Firebase.
- Las cuotas del plan gratuito limitan cuántas partidas y jugadores puede soportar el juego. Si se
  superan, habrá que revisar esta decisión.
