# Changelog

Los cambios relevantes del proyecto se recogen en este fichero.

El formato sigue [Keep a Changelog](https://keepachangelog.com/es-ES/1.1.0/) y el proyecto usa
[versionado semántico](https://semver.org/lang/es/). La versión de la app vive en `app.versionName`
de `gradle.properties`, y cada release se publica como GitHub Release con la etiqueta `v<versión>`.

## [Sin publicar]

### Añadido

- Esqueleto del proyecto Kotlin Multiplatform: motor `core` en Kotlin puro, interfaz `ui` en Compose
  Multiplatform y lanzadores para Android, iOS, escritorio y web con una pantalla de prueba.
- CI en GitHub Actions: lint, tests del motor con cobertura, build de iOS y build completo en macOS en
  las PR a `develop` (check `CI Gate`), `Tests & Lint` en las PR a `main` y flujo de releases.
- Documentación base: README, guía de contribución, arquitectura, registro de decisiones (ADR),
  licencia y este CHANGELOG.
- Modelo del mapa hexagonal en `core`: coordenadas axiales, terreno, ciudades, ríos, estrechos y
  territorios por nación, con formato JSON versionado y validación completa al cargar.
- Editor del mapa (`mapeditor`), una herramienta interna de escritorio para digitalizar el mapa sobre
  una imagen de referencia local: rejilla calibrable, pintado de terreno, territorios, ciudades, ríos
  y estrechos, y problemas del validador a la vista. En `core`, `MapDraft` y `MapJson.decodeDraft` para
  guardar y abrir mapas que todavía no son válidos.
