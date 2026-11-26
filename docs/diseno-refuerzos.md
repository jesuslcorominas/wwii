# Diseño de la tabla de refuerzos y del despliegue inicial

Este documento explica de dónde sale cada cifra del despliegue inicial y de la tabla de refuerzos de
la campaña de Europa 1939, para poder reequilibrarlas sin perder el criterio. Son cifras propias, no
las de ningún juego publicado. Los datos están en
[`data/setup/europe-1939.json`](../data/setup/europe-1939.json); el formato, en
[Despliegue y refuerzos: modelo y formato JSON](formato-despliegue.md).

## Principios

1. **Cada ficha entra una sola vez.** El catálogo ([formato-catalogo.md](formato-catalogo.md)) fija
   cuántas fichas tiene cada nación. Lo que despliega al empezar, lo que recibe en la tabla base y
   todas las bonificaciones especiales juntas no pueden pasar de esa cifra, ni siquiera con todas las
   reglas opcionales activas. Una ficha eliminada no vuelve como refuerzo.
2. **Alrededor del 40 % al empezar.** Las potencias principales despliegan unas dos quintas partes
   de sus fichas; el resto llega por turnos siguiendo la curva de producción y movilización histórica.
   Así el principio de la partida tiene frentes reconocibles sin dejar sin sentido la tabla.
3. **Menores y neutrales lo despliegan todo.** No tienen industria que pese en una guerra de seis
   años: aparecen con lo que tienen y no reciben refuerzos.
4. **La escala es relativa, no absoluta.** Una ficha terrestre es una fuerza del orden de un ejército
   pequeño, una naval una flota o escuadra con peso estratégico y una aérea una flota aérea. Lo que
   importa es la proporción entre naciones y entre años, no el número de divisiones.
5. **Reservas para los especiales.** Cuando un especial da fichas extra, la tabla base deja sin usar
   las que hacen falta para pagarlo.

## Calendario

Cada turno es un cuatrimestre. Los hitos que mueven la tabla:

| Turno | Periodo | Lo que pasa |
|---|---|---|
| 1 | sep-dic 1939 | Polonia, guerra de broma |
| 2 | ene-abr 1940 | Noruega |
| 3 | may-ago 1940 | Caída de Francia, Italia entra en guerra, batalla de Inglaterra |
| 4 | sep-dic 1940 | Italia ataca Grecia y Egipto |
| 5 | ene-abr 1941 | Norte de África, Balcanes |
| 6 | may-ago 1941 | Ataque a la URSS |
| 7 | sep-dic 1941 | Ante Moscú; EEUU entra en guerra |
| 8 | ene-abr 1942 | Primeras tropas de EEUU en Europa |
| 9 | may-ago 1942 | Ofensiva alemana del sur |
| 10 | sep-dic 1942 | Stalingrado, El Alamein, desembarco en el norte de África |
| 11 | ene-abr 1943 | Contraofensivas soviéticas |
| 12 | may-ago 1943 | Kursk, Sicilia |
| 13 | sep-dic 1943 | Italia |
| 14 | ene-abr 1944 | Preparativos del desembarco en Francia |
| 15 | may-ago 1944 | Normandía y gran ofensiva soviética de verano |
| 16 | sep-dic 1944 | Fronteras del Reich |
| 17 | ene-abr 1945 | Asalto final |
| 18 | may-ago 1945 | Fin de la guerra en Europa |

## Despliegue inicial

Totales por nación. Casi todas despliegan en su país; las excepciones son:

- Italia: 1 ficha terrestre en Libia (`ITA-LBY`) y 1 en Albania (`ITA-ALB`), el resto en Italia.
- Reino Unido: 1 terrestre y 1 naval en Egipto (`GBR-EGY`), el ejército del Nilo y la flota del
  Mediterráneo oriental; el resto en las islas.
- Francia: 1 terrestre en Argelia (`FRA-DZA`), el ejército del norte de África.

Los ids de territorio siguen la convención del mapa de Europa (código de la nación para el país,
`<NACIÓN>-<código>` para colonias y posesiones). El test JVM comprueba el despliegue contra el mapa
real en cuanto `data/maps/europe-1939.json` exista, así que cualquier id que no cuadre rompe la CI.

| Nación | Catálogo (T/N/A) | Despliega (T/N/A) | Por qué |
|---|---|---|---|
| Alemania | 30 / 3 / 6 | 12 / 2 / 3 | Ejército ya movilizado y la mejor aviación de 1939, pero lejos de su tamaño de 1941. Marina pequeña: superficie y submarinos sin grandes acorazados todavía |
| Italia | 11 / 3 / 2 | 5 / 3 / 1 | Ejército mediano repartido entre la metrópoli, Libia y Albania. Marina entera desde el principio: era su arma más fuerte y no creció |
| Finlandia, Rumanía | 2 / 0 / 0 c/u | todo | Aliados menores |
| Hungría, Bulgaria | 1 / 0 / 0 c/u | todo | Aliados menores |
| Reino Unido | 8 / 4 / 4 | 3 / 3 / 2 | Ejército profesional pequeño en 1939 y la flota más grande del teatro. El ejército crece con la Commonwealth y la movilización |
| Francia | 6 / 1 / 1 | 5 / 1 / 1 | Movilizada en 1939: casi todo su ejército está ya en el mapa |
| EEUU | 10 / 3 / 5 | nada | Entra tarde y fuera del mapa: todo llega como refuerzo |
| Noruega, Países Bajos, Bélgica, Grecia | 1 / 0 / 0 c/u | todo | Aliados menores |
| URSS | 26 / 1 / 4 | 10 / 1 / 1 | Ejército enorme pero desorganizado tras las purgas; la aviación de 1939 pesa poco. Crece mucho en 1942-44 |
| Polonia | 2 / 0 / 0 | todo | Aliado menor del bando soviético en este juego |
| Yugoslavia | 1 / 0 / 0 | todo | Aliado menor |
| España, Turquía | 3 / 0 / 0 c/u | todo | Neutrales con ejércitos grandes y poco modernos |
| Suecia | 2 / 1 / 0 | todo | Neutral armada, con flota costera |
| Suiza | 2 / 0 / 0 | todo | Neutral armada en un terreno muy defendible |

Dinamarca, Irlanda y Portugal no tienen fichas.

## Refuerzos por nación

T = tierra, N = naval, A = aérea.

### Alemania (despliega 12/2/3; recibe 16/1/3; reserva 2 T)

| Turno | Fichas | Por qué |
|---|---|---|
| 2 | 2 T | Nuevas divisiones antes de la campaña del oeste |
| 3 | 2 T, 1 A | Divisiones blindadas nuevas y la aviación en su máximo para Francia e Inglaterra |
| 4 | 1 T | Reorganización tras Francia |
| 5 | 2 T, 1 N | Preparación de la campaña del este; el gran acorazado entra en servicio |
| 6 | 3 T, 1 A | Máximo esfuerzo para el ataque a la URSS |
| 8 | 1 T | Reposición tras el invierno ante Moscú |
| 9 | 2 T | Movilización de la industria de guerra para la ofensiva del sur |
| 12 | 1 T | Nuevos blindados para la última ofensiva en el este |
| 13 | 1 A | Más cazas para defender el Reich de los bombardeos |
| 14 | 1 T | Divisiones nuevas ante el desembarco que se espera |
| 15 | 1 T | Leva de emergencia tras el verano de 1944 |

Quedan 2 fichas terrestres sin usar en la tabla base: son las de la regla opcional de ciudades
clave (Suez y Stalingrado). Total con todo activo: 30/3/6.

### Italia (despliega 5/3/1; recibe 6/0/1)

| Turno | Fichas | Por qué |
|---|---|---|
| 3 | 2 T | Entrada en guerra y refuerzo de Libia |
| 4 | 1 T, 1 A | Campañas de Grecia y Egipto |
| 5 | 1 T | Refuerzos para África y los Balcanes |
| 7 | 1 T | Cuerpo expedicionario en el este |
| 9 | 1 T | Ejército en el este, el último esfuerzo grande |

Desde 1943 Italia no recibe nada: su industria no da para más. Total: 11/3/2.

### Reino Unido (despliega 3/3/2; recibe 4/1/2; reserva 1 T)

| Turno | Fichas | Por qué |
|---|---|---|
| 3 | 1 A | Cazas para la batalla de Inglaterra |
| 4 | 1 T | Tropas de la Commonwealth en Egipto |
| 6 | 1 T | Más tropas de la Commonwealth en el Mediterráneo |
| 8 | 1 N | Flota reconstruida y escoltas del Atlántico |
| 9 | 1 T | Refuerzo del ejército del desierto antes de El Alamein |
| 10 | 1 A | Ofensiva de bombardeo |
| 14 | 1 T | Ejército para el desembarco en Francia |

Queda 1 ficha terrestre para la ciudad clave de Roma. Total con todo activo: 8/4/4.

### Francia (despliega 5/1/1; recibe 1/0/0)

| Turno | Fichas | Por qué |
|---|---|---|
| 2 | 1 T | Final de la movilización |

Después de 1940 Francia no recibe nada: lo que quede de su ejército sigue luchando con los
Occidentales. Total: 6/1/1.

### EEUU (despliega nada; recibe 10/3/5)

| Turno | Fichas | Por qué |
|---|---|---|
| 8 | 1 T, 1 N, 1 A | Primeras tropas y aviones en Gran Bretaña; flota del Atlántico |
| 9 | 1 A | Primeros grupos de bombarderos |
| 10 | 2 T, 1 N | Desembarco en el norte de África |
| 11 | 1 T, 1 A | Túnez |
| 12 | 1 T | Sicilia |
| 13 | 1 T, 1 A | Italia y la ofensiva aérea |
| 14 | 2 T, 1 N, 1 A | Concentración para el desembarco en Francia |
| 15 | 1 T | Segundo desembarco en el sur de Francia |
| 16 | 1 T | Últimas divisiones para el frente occidental |

EEUU empieza en el turno 8 (decisión de diseño): entra en guerra a finales de 1941, pero sus fuerzas
no pesan en Europa hasta 1942. Total: 10/3/5.

### URSS (despliega 10/1/1; recibe 15/0/3; reserva 1 T)

| Turno | Fichas | Por qué |
|---|---|---|
| 2 | 1 T | Reformas tras la guerra con Finlandia |
| 4 | 1 T | Rearme ante la amenaza alemana |
| 6 | 2 T | Movilización general tras el ataque |
| 7 | 2 T, 1 A | Divisiones de Siberia ante Moscú; fábricas trasladadas al este |
| 8 | 1 T | Contraofensiva de invierno |
| 9 | 1 T | Nuevos ejércitos para el verano de 1942 |
| 10 | 2 T, 1 A | Concentración para la contraofensiva de Stalingrado |
| 11 | 1 T | Persecución de invierno |
| 12 | 1 T, 1 A | Reservas estratégicas para Kursk |
| 13 | 1 T | Avance hasta el Dniéper |
| 14 | 1 T | Ofensivas de invierno |
| 15 | 1 T | Gran ofensiva de verano de 1944 |

Queda 1 ficha terrestre para la ciudad clave de Bucarest. La marina soviética no crece: en este
teatro pesó poco. Total con todo activo: 26/1/4.

## Refuerzos especiales (reglas opcionales del Avanzado)

Cada especial tiene un id estable. El reglamento decide cuáles se activan; el Clásico no activa
ninguno.

| Id | Qué hace | Por qué |
|---|---|---|
| `axis-minors-late-entry` | Rumanía, Hungría y Finlandia no despliegan al empezar y entran en el turno 6; Bulgaria, en el 5 | Estos países no entraron en guerra en 1939: lo hicieron con la campaña de los Balcanes y el ataque a la URSS. La opción quita al Eje fuerza al principio a cambio de realismo |
| `spanish-volunteers` | 1 ficha terrestre española no despliega y entra en el turno 7 luchando por el Eje; España sigue neutral | Voluntarios enviados al frente del este sin entrar en guerra |
| `key-cities` | +1 ficha terrestre, una sola vez, la primera vez que en la fase de refuerzos el bando controla la ciudad: Suez (`suez`) o Stalingrado (`stalingrad`) para el Eje (Alemania), Roma (`rome`) para los Occidentales (Reino Unido) y Bucarest (`bucharest`) para la URSS | Premian los objetivos estratégicos: el canal, el Volga, la capital italiana y el petróleo rumano |
| `spain-joins-axis` | Si el Eje controla Gibraltar (`gibraltar`) en la fase de refuerzos, España se une al Eje con las fichas que tenga | La caída del Peñón habría cambiado el cálculo español. No añade fichas |

Los especiales que reparten fichas (aplazamiento y voluntarios) no crean fichas nuevas: mueven en el
tiempo las que ya se despliegan. Las bonificaciones de ciudades clave sí añaden fichas y por eso la
tabla base deja una reserva.

Las ciudades se referencian por un id estable (entre paréntesis en la tabla), no por su nombre
visible.

Evaluar las condiciones durante la partida (quién controla una ciudad, si ya se cobró una
bonificación) y el cambio de bando de España son cosa del motor de reglas y de la diplomacia de
neutrales, no de estos datos. Ver [0013](decisiones/0013-despliegue-y-refuerzos-como-dato.md).

## Cómo reequilibrar

- **Mover una ficha de turno** no cambia ningún total: solo el ritmo. Es el ajuste más seguro.
- **Cambiar cuántas fichas despliega una nación** obliga a cambiar sus refuerzos en sentido contrario,
  o a cambiar el catálogo.
- **Añadir una bonificación** obliga a sacar una ficha de la tabla base o a ampliar el catálogo. La
  validación rechaza el fichero si una nación pide más fichas de las que tiene en el peor caso.
- **Comprobar**: `./gradlew :core:jvmTest` valida el fichero real contra el catálogo (y contra el mapa
  cuando exista) y comprueba los totales de este documento.
