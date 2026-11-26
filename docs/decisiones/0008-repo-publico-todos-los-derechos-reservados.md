# 0008. Repositorio público con todos los derechos reservados

- **Estado:** Aceptada
- **Fecha:** 2026-11-25

## Contexto

- **Visibilidad:** en el plan gratuito de GitHub, un repositorio público tiene minutos ilimitados de
  GitHub Actions, incluidos los runners de macOS que necesita el build de iOS, y protección de ramas.
  Uno privado no tiene ninguna de las dos cosas.
- **Licencia:** el juego es un proyecto personal que se quiere publicar en tiendas. Su nombre, mapa,
  gráficos, reglas y código son propios. Publicar el código no debe permitir que otros lo reutilicen o
  publiquen copias del juego.

## Decisión

- El repositorio es **público**.
- **No tiene licencia de código abierto**: todos los derechos reservados a favor de Jesús López
  Corominas. El fichero [LICENSE](../../LICENSE) lo declara y el README lo indica en su sección de
  licencia.
- El código se puede leer, pero no se concede permiso para usarlo, copiarlo, modificarlo ni
  distribuirlo.

## Alternativas consideradas

- **Repositorio privado:** sin Actions ilimitado ni protección de ramas en el plan gratuito. La CI de
  iOS sería inviable.
- **Licencia de código abierto (MIT, Apache 2.0, GPL...):** permitiría reutilizar el código o
  publicar copias del juego.
- **Licencia "source-available" con condiciones propias:** añade complejidad legal sin una necesidad
  clara por ahora.

## Consecuencias

- La CI completa, con iOS, y la protección de `develop` y `main` no tienen coste.
- Todo lo que entra en el repo es visible: nunca credenciales, claves ni datos personales. Los
  secretos van en los secrets de GitHub Actions.
- El repo no puede contener material de terceros (textos, mapas, fichas o tablas de otros juegos).
- Las contribuciones externas no están abiertas. Si se abrieran, haría falta un acuerdo de cesión de
  derechos.
