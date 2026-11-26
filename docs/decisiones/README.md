# Registro de decisiones

Cada decisión de arquitectura o de producto que condiciona el proyecto se recoge en un ADR
(*Architecture Decision Record*): una nota corta con el contexto, la decisión, las alternativas que
se descartaron y sus consecuencias.

## Decisiones

| N.º | Decisión | Estado |
|---|---|---|
| [0001](0001-kotlin-multiplatform-y-compose.md) | Kotlin Multiplatform y Compose Multiplatform para las cuatro plataformas | Aceptada |
| [0002](0002-motor-puro-y-determinista.md) | Motor del juego en Kotlin puro y determinista | Aceptada |
| [0003](0003-reglamento-configurable.md) | Reglamento configurable: Clásico y Avanzado | Aceptada |
| [0004](0004-sin-backend-propio.md) | Sin backend propio: Firebase en plan gratuito detrás de interfaces | Aceptada |
| [0005](0005-online-asincrono-con-plazo.md) | Online asíncrono con plazo por turno | Aceptada |
| [0006](0006-escenarios-como-modo-rapido.md) | Escenarios cortos como modo rápido | Aceptada |
| [0007](0007-layout-por-tamano-de-ventana.md) | Layout por tamaño de ventana | Aceptada |
| [0008](0008-repo-publico-todos-los-derechos-reservados.md) | Repositorio público con todos los derechos reservados | Aceptada |
| [0009](0009-modelo-del-mapa-hexagonal.md) | Modelo del mapa hexagonal: coordenadas axiales, estrechos como hex de mar y bando fuera del mapa | Aceptada |
| [0010](0010-borradores-de-mapa-para-el-editor.md) | Borradores de mapa sin validar para el editor, y calibración fuera del mapa | Aceptada |
| [0011](0011-catalogo-de-naciones-como-dato.md) | Catálogo de naciones como dato y carpeta `data/` para los datos del juego | Aceptada |

## Cómo añadir un ADR

1. Copia la plantilla de abajo en `NNNN-titulo-corto.md`, con el siguiente número libre.
2. Rellénala y añádela a la tabla de arriba en la misma PR que aplica la decisión.
3. Un ADR aceptado no se reescribe. Si la decisión cambia, se crea uno nuevo que la sustituye y el
   antiguo pasa a estado "Sustituida por NNNN", con un enlace al nuevo.

Estados posibles: Propuesta, Aceptada, Sustituida, Descartada.

```markdown
# NNNN. Título

- **Estado:** Propuesta
- **Fecha:** AAAA-MM-DD

## Contexto

Qué problema o fuerza obliga a decidir.

## Decisión

Qué se decide, en presente.

## Alternativas consideradas

- Opción: por qué no.

## Consecuencias

Qué se gana, qué se pierde y qué obliga a hacer a partir de ahora.
```
