# Índice de documentación — LibroMágico

Punto de entrada a toda la documentación del proyecto. La fuente de verdad es el código fuente y las migraciones de Flyway (`src/main/resources/db/migration`); si un documento contradice el código, el código gana.

## Cómo usar este índice

1. **¿Qué es y para quién es?** → empieza por [`docs/product/project-charter.md`](product/project-charter.md).
2. **¿Cómo funciona por dentro?** → [`docs/architecture/c4.md`](architecture/c4.md) y los [ADRs](architecture/adr/README.md).
3. **¿Cómo lo levanto y lo opero?** → sección [Operativa](#operativa).
4. **¿Qué puedo hacer según mi rol?** → sección [Usuarios](#usuarios).
5. **¿Cómo contribuyo?** → [`CONTRIBUTING.md`](../CONTRIBUTING.md).

## Producto

| Documento | Contenido |
|---|---|
| [Project charter](product/project-charter.md) | Visión, alcance (dentro/fuera), objetivos, stakeholders, métricas de éxito |
| [Requisitos (SRS)](product/requirements.md) | Requisitos funcionales por módulo (RF-XX) y no funcionales, con criterios de aceptación |
| [Roadmap](product/roadmap.md) | Fases: MVP entregado y propuestas futuras (reservas, reportes, pagos, PWA...) |

## Arquitectura

| Documento | Contenido |
|---|---|
| [C4 (contexto → contenedores → componentes)](architecture/c4.md) | Diagramas Mermaid para entender el sistema en minutos |
| [ADRs](architecture/adr/README.md) | Decisiones de arquitectura registradas (11 ADRs): monolito, JWT en cookie, Flyway, backup, etc. |
| [Modelo de datos (ER)](architecture/data-model.md) | Diagrama entidad-relación, tablas y migraciones V1–V4 |
| [Flujos de negocio](architecture/business-flows.md) | Diagramas de proceso: préstamo, devolución, multas, auth, reset de contraseña |

## Seguridad y cumplimiento

| Documento | Contenido |
|---|---|
| [Índice de seguridad](security/README.md) | Alcance de la sección |
| [RBAC (roles y permisos)](security/rbac.md) | Matriz USER/LIBRARIAN/ADMIN por endpoint, reglas de `SecurityConfig` y ownership |
| [Datos personales y Habeas Data](security/data-protection.md) | Qué datos se tratan, cómo se protegen y alineación con la Ley 1581 de 2012 (Colombia) |

## Operativa

| Documento | Contenido |
|---|---|
| [Backup y restore](ops/backup-restore.md) | Estrategia RPO 24 h / RTO < 30 min, scripts y guía de restore |
| [Despliegue](ops/deployment.md) | Runbook de despliegue productivo, verificación y rollback |
| [Infraestructura](ops/infrastructure.md) | Docker, CI/CD, volúmenes, observabilidad |
| [Incidentes](ops/incident-response.md) | Niveles de severidad, runbooks por incidente y plantilla de postmortem |

## Pruebas

| Documento | Contenido |
|---|---|
| [Estrategia de testing](testing/strategy.md) | Pirámide unit → integración → E2E, cobertura JaCoCo ≥ 70 %, comandos |
| [Casos críticos](testing/critical-cases.md) | Escenarios de prueba críticos (RBAC, ownership, multas, rate limit, revocación) |

## Usuarios

| Documento | Audiencia |
|---|---|
| [Guía del usuario final](users/user-guide.md) | Lectores: catálogo, préstamos, devoluciones, multas, contraseña |
| [Guía del bibliotecario](users/librarian-guide.md) | Gestión de libros, préstamos y devoluciones |
| [Guía del administrador](users/admin-guide.md) | Usuarios, roles, bloqueos, multas y panel |

## Técnica

| Documento | Contenido |
|---|---|
| [README](../README.md) | Inicio rápido, stack, tests, despliegue |
| [CHANGELOG](../CHANGELOG.md) | Historial de cambios por versión |
| [API (Swagger/OpenAPI)](../README.md#documentación) | `/swagger-ui.html` (requiere autenticación) y `/v3/api-docs` |

## Convenciones

- Los documentos técnicos se escriben en español neutro, salvo que el contexto indique otra cosa.
- Cualquier cambio de comportamiento debe actualizar los docs afectados en el **mismo PR**.
- Las decisiones de arquitectura nuevas se registran como ADR (ver [`architecture/adr/README.md`](architecture/adr/README.md)).