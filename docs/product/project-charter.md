# Project Charter — LibroMágico

Sistema de gestión bibliotecaria para bibliotecas pequeñas y medianas. LibroMágico digitaliza el ciclo completo de operación bibliotecaria sobre un solo monolito: catálogo, préstamos, devoluciones, multas y administración. Está operativo hoy y se despliega con Docker Compose.

Referencia técnica y de instalación: [README.md](../../README.md).

## Visión del producto

Las bibliotecas pequeñas y medianas suelen gestionar préstamos, devoluciones y morosidad en planillas manuales u hojas de cálculo. Eso vuelve lento cada operación, hace difícil saber qué hay disponible y deja la recaudación de multas sin control.

LibroMágico resuelve ese problema con una herramienta única de autoservicio:

- **El lector** consulta el catálogo, toma prestado y devuelve, y paga multas por su cuenta.
- **El bibliotecario** administra catálogo, préstamos, multas y usuarios; el sistema vigila los vencimientos y cobra la multa automáticamente.
- **El administrador** tiene visibilidad total con un dashboard y control sobre usuarios y libros.

## Para quién

| Actor | Necesidad |
|---|---|
| Bibliotecarios | Manejar catálogo, préstamos/devoluciones y morosidad sin papel |
| Administradores | Controlar usuarios, libros, multas y métricas generales |
| Usuarios lectores | Buscar libros, pedir prestado, devolver y pagar multas sin depender del personal |

## Alcance

### Dentro del alcance (IN)

- **Catálogo público** con búsqueda multi-campo y paginación.
- **Autenticación** por JWT en cookie `httpOnly`: registro, login y logout, con CSRF y rate limiting.
- **Recuperación de contraseña** por email con token.
- **Préstamos** de 15 días con ownership por usuario.
- **Devoluciones** con multa plana de $10 por demora.
- **Multas** con estados Pendiente/Pagado (autoservicio y administración).
- **Administración**: gestión de usuarios (rol/estado), libros (CRUD + marcar Perdido), listados de préstamos y multas, y dashboard con estadísticas.
- **Seguridad** defensiva: CSP, denylist de JWT, Habeas Data.
- **Operación**: CI/CD, cobertura ≥ 70 % (JaCoCo) y backup diario.

### Fuera del alcance (OUT)

- Reservas de libros (el estado `RESERVADO` es únicamente un valor legal del enum, no hay flujo de reservas).
- Notificaciones push.
- Aplicación móvil dedicada.
- Pasarela de pago real (las multas se registran internamente, no se cobran con dinero real).
- Importación masiva de libros.
- Multi-sucursal.

## Objetivos medibles

| Objetivo | Métrica / meta |
|---|---|
| Reducir el tiempo de gestión de un préstamo | Registro de préstamo en segundos, sin papel ni formularios duplicados |
| Visibilidad del catálogo | Búsqueda multi-campo con paginación; cualquier lector consulta disponibilidad sin intermediarios |
| Control de morosidad | Detección automática de préstamos ATRASADOS (scheduler horario) y multa plana automática al devolver tarde |
| Autoservicio | Usuario puede pedir, devolver y pagar multas sin intervención del staff |
| Operación confiable | Cobertura de tests ≥ 70 % (barrera JaCoCo), suite E2E y backup diario (RPO 24 h, RTO < 30 min) |

## Stakeholders y roles de usuario

Exactamente tres roles:

| Rol | Alcance |
|---|---|
| `USER` | Cliente de la biblioteca: catálogo, préstamo/devolución y multas propias |
| `LIBRARIAN` | Gestión de catálogo, préstamos, multas y usuarios |
| `ADMIN` | Todo lo anterior más control de roles/estado de usuarios |

Stakeholders: dirección de la biblioteca (resultados), bibliotecarios y administradores (operación diaria), usuarios lectores (experiencia de uso), equipo de desarrollo y mantenimiento (operación del sistema).

## Éxito del producto / métricas

- **Adopción**: usuarios registrados y préstamos activos creciendo.
- **Eficiencia**: reducción del tiempo por préstamo/devolución frente al proceso manual.
- **Morosidad**: multas pendientes detectables y cobrables desde el sistema; lista de ATRASADOS actualizada.
- **Disponibilidad**: el sistema responde 24/7; backup diario y restore probado.
- **Calidad**: cobertura de tests ≥ 70 % y suite E2E verde en CI.

## Restricciones

| Restricción | Implicación |
|---|---|
| Monolito Spring Boot 3.2.5 (Java 17) | Una artefacto sirve API REST y SPA; despliegue simple |
| Frontend en JavaScript vanilla (sin frameworks) | Sin dependencias de build de frontend; router propio en el cliente |
| PostgreSQL 16 en producción (H2 en memoria en dev) | Migraciones con Flyway; entorno dev sin configuración previa |
| Docker Compose como vía de despliegue | `docker-compose.prod.yml` para producción con secretos obligatorios |
| Equipo pequeño | Arquitectura simple, stack conservador, sin piezas externas innecesarias |
| Contexto legal colombiano | Cumplimiento de Habeas Data en el tratamiento de datos personales |

## Referencias

- [README.md](../../README.md) — características, stack, instrucciones de desarrollo y despliegue.
- `docs/ops/backup-restore.md` — estrategia de backup y restore (RPO 24 h, RTO < 30 min).