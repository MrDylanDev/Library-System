# ADR-010: SPA vanilla JS sin framework ni build

- **Estado:** Aceptado
- **Fecha estimada:** 2025-Q2
- **Relacionados:** [ADR-001](ADR-001-monolito-spring-boot.md)

## Contexto

El frontend de LibroMágico debe ser servido por el mismo artefacto que la API (ver [ADR-001](ADR-001-monolito-spring-boot.md)). Las alternativas clásicas (React/Vue + Vite) introducen un paso de build, dependencias de node_modules y un artefacto estático separado que versionar y servir, lo que complica el despliegue de un equipo pequeño.

## Decisión

Construir la interfaz como una **SPA vanilla JavaScript**, sin framework y sin paso de build:

- **Router por hash propio** (la ruta vive en `#/...`), sin necesidad de configuración de servidor para rutas limpias.
- Módulos propios: **`lib.js`**, **`api.js`**, **`app.js`** y un conjunto de componentes JS.
- **Estado de sesión/UI gestionado manualmente** con un `Store` apoyado en `localStorage` (sin librería de estado).
- La SPA se sirve desde el propio contexto de Spring Boot (`/`, `/index.html`, `/css/**`, `/js/**`), junto a la API, y usa la cookie JWT ([ADR-002](ADR-002-jwt-cookie-httpOnly.md)) y el header CSRF ([ADR-003](ADR-003-csrf-cookie.md)).
- Estilos propios: **1093 líneas de CSS**.

## Alternativas consideradas

- **React/Vue + Vite con separación de despliegue:** mayor ecosistema y ergonomía de desarrollo, pero obliga a un artefacto estático separado, un pipeline de build y un servidor de estáticos; coste desproporcionado para este proyecto.
- **Framework + build integrado en Spring Boot:** reduce la separación pero conserva el coste de toolchain de node y la complejidad de versionado; rechazado por simplicidad.

## Consecuencias

- **Cero build:** no hay paso de compilación de frontend; se sirven archivos estáticos tal cual.
- **Servido por el mismo contexto:** la API y la SPA comparten origen, lo que simplifica CORS y CSRF.
- **Complejidad de estado manual:** al no haber framework, el manejo de estado, renderizado y enrutado se gestiona a mano (Store en `localStorage`, router por hash); requiere disciplina en `lib.js`/`api.js`/`app.js`.
- **CSS mantenido a mano:** 1093 líneas de CSS sin preprocesador; el mantenimiento es directo pero manual.
- **Sin test runner de frontend:** la cobertura de la interfaz recae en los E2E con Playwright + Python (ver [c4.md](../c4.md)).