# LibroMágico

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

Sistema de gestión de bibliotecas: catálogo, préstamos, multas y panel de administración.

El backend es una aplicación Spring Boot que sirve la API REST y el frontend (SPA en JavaScript vanilla) desde el mismo contexto web. Con Docker Compose se levanta el stack completo en desarrollo; el perfil `dev` siembra datos de prueba automáticamente para explorar la aplicación sin configuración previa.

## Características

- **Catálogo con búsqueda y paginación**: listado de libros con filtro por texto que se mantiene entre páginas.
- **Préstamos y devoluciones**: estados de préstamo *Activo / Devuelto / Atrasado* y estado del libro *Disponible / Prestado / Reservado / Perdido*.
- **Multas con pago**: se generan al devolver con demora; estados *Pendiente / Pagado*.
- **Autenticación JWT**: token en cookie `httpOnly`, protección CSRF, limitación de intentos (rate limiting) en los endpoints de autenticación y revocación server-side del token.
- **Roles**: `USER`, `LIBRARIAN` y `ADMIN`, con permisos por ruta en el frontend y por endpoint en el backend.
- **Panel de administración**: dashboard, CRUD de libros y gestión de usuarios, multas y préstamos.
- **Emails transaccionales**: MailHog en desarrollo, SMTP real en producción (registro, confirmaciones, reset de contraseña).
- **Observabilidad**: Spring Boot Actuator con endpoints de salud, información y métricas.

## Stack

| Componente | Tecnología |
|---|---|
| Backend | Java 17, Spring Boot 3.2 (Spring Security, Spring Data JPA, Spring Mail) |
| Base de datos | PostgreSQL 16, migraciones con Flyway |
| Frontend | SPA en JavaScript vanilla (sin framework), router propio en el cliente |
| Contenedores | Docker Compose (desarrollo y producción) |
| Tests E2E | Playwright + pytest |
| Build | Maven (wrapper `./mvnw`) |
| CI | GitHub Actions |

## Arquitectura resumida

El proyecto es un monolito Spring Boot: la API REST y los recursos estáticos del frontend se sirven desde el mismo artefacto. El frontend es una SPA en JavaScript vanilla con router hash propio (`Router` en `static/js/app.js`) que consume la API usando el token JWT en cookie `httpOnly`. La base de datos es PostgreSQL cuyo esquema gestiona Flyway.

```
Cliente (SPA vanilla JS)
        │  HTTP + JSON (JWT en cookie httpOnly)
        ▼
Spring Boot (API REST + seguridad)
        │
        ├──▶ PostgreSQL 16 (Flyway)
        └──▶ MailHog (dev) / SMTP (prod)
```

Código principal:

- Backend: `src/main/java/com/libromagico/` → `config`, `controller`, `service`, `repository`, `model`, `security`.
- Frontend: `src/main/resources/static/js/` → `components/` (`catalog.js`, `loans.js`, `admin.js`, etc.). Rutas: `#/catalogo`, `#/libros/{isbn}`, `#/mis-prestamos`, `#/mis-multas`, `#/admin`, `#/login`, `#/registro`.
- End-to-end: `tests/e2e/`.

## Iniciar el entorno de desarrollo

Requisito: Docker con Compose v2.

```bash
docker compose up -d --build
```

Servicios del stack dev:

| Servicio | URL / puerto |
|---|---|
| App | <http://localhost:8080> |
| MailHog (UI de correo) | <http://localhost:8025> |
| PostgreSQL | `localhost:5432` (usuario/BD `libromagico`) |

Verificación de salud:

```bash
curl http://localhost:8080/api/health
```

El perfil `dev` siembra datos de prueba automáticamente en el primer arranque: **34 libros** y **4 usuarios**. Credenciales de los usuarios seed:

| Rol | Email | Contraseña | Notas |
|---|---|---|---|
| ADMIN | `admin@libromagico.com` | `admin123` | Acceso total |
| LIBRARIAN | `librarian@libromagico.com` | `librarian123` | Gestión del catálogo, usuarios, multas y préstamos |
| USER | `usuario@libromagico.com` | `usuario123` | Usuario estándar |
| USER | `moroso@libromagico.com` | `moroso123` | Creado por el seed de deudas; incluye 2 multas pendientes |

Notas:

- El primer build tarda: Maven compila dentro del contenedor. Los volúmenes `maven-repo` y `postgres-data` cachean las siguientes corridas.
- Docker Compose lee `.env` automáticamente si existe. `POSTGRES_PASSWORD` y `JWT_SECRET` tienen valores por defecto para desarrollo (ver `.env.example`).
- En este stack dev la app se conecta a PostgreSQL. Si se ejecuta la app sin Compose (`./mvnw spring-boot:run`), el perfil por defecto usa una base H2 en memoria.

## Ejecutar los tests

Tests de backend:

```bash
./mvnw test
```

Con la barrera de cobertura (JaCoCo, mínimo 70 % de instrucciones) y el reporte:

```bash
./mvnw -Pcoverage verify
```

Suite E2E (requiere el stack dev arriba y healthy):

```bash
cd tests/e2e
pip install -r requirements.txt
python -m playwright install chromium --with-deps
python -m pytest
```

La suite apunta a `http://localhost:8080` y usa MailHog de `http://localhost:8025` (definidos en `conftest.py`).

> **Importante**: la suite E2E **muta la base** (crea préstamos, multas y usuarios). Entre corridas deja los datos sucios; para volver a datos frescos:

```bash
docker compose down -v && docker compose up -d --build
```

## Despliegue productivo

1. Crear y completar los secretos:

   ```bash
   cp .env.prod.example .env.prod
   ```

2. Levantar el stack de producción:

   ```bash
   docker compose --env-file .env.prod -f docker-compose.prod.yml -p libromagico up -d --build
   ```

Diferencias clave con el entorno de desarrollo:

- La cookie se envía con el flag `Secure` y el SQL de Hibernate se oculta (`SPRING_PROFILES_ACTIVE=prod`).
- La imagen es autocontenida (multi-stage: build Maven → JRE 17) y arranca con contexto `prod`; no monta código fuente.
- La base de datos **no expone puertos al host** (solo red interna de Docker).
- Los emails salen por **SMTP real** y Actuator expone `health`, `info` y `metrics`.
- **Backup diario automático** de la base a las 02:00 (retención configurable) — ver `docs/ops/backup-restore.md`.

Los secretos son obligatorios: si falta alguno, el stack falla rápido con error (`${VAR:?}`).

## CI (GitHub Actions)

En cada push a `main` y en cada pull request corren 3 jobs:

| Job | Qué valida |
|---|---|
| Backend tests (Maven) | Suite completa con `mvn -B -Pcoverage verify` (barrera JaCoCo ≥ 70 %) y reporte de cobertura |
| E2E (Playwright) | Suite E2E completa contra el stack real (PostgreSQL + MailHog) levantado con Docker Compose |
| Backup/Restore (PostgreSQL) | Los scripts reales `backup.sh` y `restore.sh` sobre Postgres: dump → restore → verificación de datos y retención |

## Documentación

La documentación completa del proyecto vive en `docs/` — [índice general](docs/INDEX.md):

- **Producto**: [charter](docs/product/project-charter.md), [requisitos (SRS)](docs/product/requirements.md), [roadmap](docs/product/roadmap.md).
- **Arquitectura**: [C4](docs/architecture/c4.md), [ADRs](docs/architecture/adr/README.md), [modelo de datos](docs/architecture/data-model.md), [flujos de negocio](docs/architecture/business-flows.md).
- **Seguridad**: [RBAC](docs/security/rbac.md) y [dato personal / Habeas Data](docs/security/data-protection.md).
- **Operativa**: [despliegue](docs/ops/deployment.md), [infraestructura](docs/ops/infrastructure.md), [incidentes](docs/ops/incident-response.md), [backup y restore](docs/ops/backup-restore.md).
- **Pruebas**: [estrategia](docs/testing/strategy.md) y [casos críticos](docs/testing/critical-cases.md).
- **Manuales**: [usuario](docs/users/user-guide.md), [bibliotecario](docs/users/librarian-guide.md), [administrador](docs/users/admin-guide.md).

## Licencia

MIT — ver [LICENSE](LICENSE).