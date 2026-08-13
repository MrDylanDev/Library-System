# Changelog

Todas las versiones notables de **LibroMágico** se documentan en este archivo.

El formato sigue las pautas de [Keep a Changelog](https://keepachangelog.com/es-ES/1.1.0/), y
el versionado semántico se aplica según [SemVer](https://semver.org/lang/es/). Los cambios se
agrupan en: **Añadido** (funcionalidades nuevas), **Corregido** (bugs), **Cambiado**
(modificaciones de comportamiento existente), **Seguridad** (endurecimiento) y **Operaciones**
(infraestructura, CI, observabilidad).

La numeración de versiones de este proyecto inicia en la **0.1.0**; la versión 1.0.0 se
publicará cuando se alcance un estado de funcionalidad estable para producción.

[Releases](https://github.com/MrDylanDev/Library-System/releases)

## [0.1.0] - 2026-08-12

Primera versión funcional de LibroMágico: sistema de gestión de biblioteca con autenticación
JWT, catálogo público, préstamos, multas y panel administrativo, desplegable en producción con
Docker y cubierto por CI (unit, E2E y backup/restore).

### Añadido

- Modelo de datos y configuración inicial del proyecto (Spring Boot 3.2.5, Java 17, PostgreSQL 16).
- Autenticación JWT con registro, inicio y cierre de sesión, y roles USER / LIBRARIAN / ADMIN.
- Repositorios, DTOs y capa de excepciones para los dominios del sistema.
- Servicios, controllers y manejador global de excepciones para la API REST.
- Tests de integración y datos seed de desarrollo (34 libros, 3 usuarios, usuario moroso con multas).
- Frontend SPA (vanilla JS servido por el mismo artefacto) con landing page.
- Servicio de email con JavaMailSender (HTML, envío asíncrono).
- Endpoints de administración para gestión de roles, estados de usuario y multas.
- Rediseño de la landing page.
- Catálogo público con búsqueda (título/autor/categoría/ISBN) y paginación.
- Recuperación de contraseña por correo.
- Transaccionalidad en servicios y borrado seguro de libros con préstamos activos.
- Cobertura de código con JaCoCo.
- Endpoint de salud `/api/health`.
- Logging estructurado con MDC y correlation IDs, más configuración de devcontainer.
- Tests de controllers y validación de DTOs.
- Documentación interactiva de la API con Swagger/OpenAPI y esquema de autenticación JWT.
- Perfiles de configuración `dev` y `prod`.
- Paginación y ordenamiento en todos los endpoints de listado.
- Endpoint `GET /api/multas/mis-multas` y página "Mis Multas" con pago propio de multas.
- Vista de préstamos activos para administrador y bibliotecario.
- Modales de confirmación para las acciones del panel administrativo.
- Configuración SMTP de producción con remitente configurable.
- Estado PERDIDO para libros, con validación en el flujo de préstamos.
- Dashboard administrativo con estadísticas (9 tarjetas).
- Suite de pruebas E2E con Playwright para los flujos principales de la SPA.
- Rate limiting en login, registro y recuperación de contraseña.
- Revocación de JWT en el servidor mediante denylist.
- Migraciones de esquema con Flyway y validación de integridad.
- Health check real de la base de datos (SELECT 1 con timeout).

### Corregido

- Flujo de sesión del usuario: corregido el endpoint `GET /api/auth/me`.
- Sustituida la validación de año hardcodeada (`@Max(2026)`) por la validación dinámica `@NotFutureYear`.
- Préstamos vencidos marcados automáticamente como ATRASADO.
- Configuración de CORS con orígenes permitidos configurables.
- Devolución de préstamos ATRASADOS y bugs del frontend detectados por la suite E2E.
- Retorno del DTO `UsuarioResponse` en los endpoints de administración.
- Transaccionalidad añadida a `MultaService` y `UsuarioService`.
- Filtro de préstamos activos y borrado seguro de libros.
- Ownership de préstamos: se devuelve 403 al acceder a préstamos de otro usuario.
- Tests frágiles refactorizados y calidad de cobertura mejorada.

### Cambiado

- El JWT se migró de la respuesta del login a una cookie `httpOnly`.
- Perfiles de configuración `dev` y `prod` separados.
- Validación de esquema de base de datos activada con Flyway.
- Paginación añadida al catálogo y visibilidad de acciones administrativas en el detalle del libro.

### Seguridad

- Cabeceras CSP y consola H2 restringida al perfil `dev`.
- Rate limiting en los endpoints de autenticación (login, registro, recuperación de contraseña).
- Revocación de tokens JWT en el servidor mediante denylist.
- Secreto JWT externalizado a variables de entorno (generado con `openssl rand -base64 48`).
- El login ya no devuelve el JWT en el cuerpo de la respuesta.
- CORS restringido a orígenes permitidos configurables por entorno.
- Higiene de secretos en producción: variables obligatorias exigidas en el stack de Docker.

### Operaciones

- Stack productivo Docker: imagen multi-stage con usuario no-root y servicios `app`, `db` y `backup`.
- Pipeline de CI con tres jobs: tests con gate de cobertura (≥70%), suite E2E con Playwright y verificación de backup/restore.
- Observabilidad con Spring Boot Actuator (health, info y metrics).
- Health check real de la base de datos para el healthcheck del contenedor.
- Estrategia de backup/restore de PostgreSQL (RPO 24 h / RTO < 30 min) con dump diario por cron.
- Logging con correlation IDs y rolling de archivos de log (10 MB, 7 días, tope 10 GB).
