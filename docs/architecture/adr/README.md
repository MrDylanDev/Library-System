# Registro de Decisiones de Arquitectura (ADRs)

Este directorio mantiene el registro de decisiones de arquitectura (Architecture Decision Records, ADRs) de LibroMágico: un historial por escrito de **qué** se decidió, **por qué** y **qué consecuencias** trajo cada decisión importante.

## Qué es un ADR y cómo se usa en este repo

Un ADR captura una decisión relevante y su razonamiento en el momento en que se toma, para que una persona que trabaje sobre el código meses después no tenga que reconstruir la justificación a partir del código (o adivinarla).

En este repositorio se usa una adaptación del formato clásico de Michael Nygard:

- **Uno por decisión importante.** Cada ADR documenta exactamente una decisión que afecta a la arquitectura: elección de tecnología, modelo de seguridad, esquema de datos, etc. Las decisiones menores que no cambian la forma del sistema van en el código o en el commit correspondiente, no aquí.
- **Numeración secuencial.** Los ADR se numeran `ADR-XXX` en orden de creación. El número no cambia si una decisión se revisa después; la revisión se registra como un ADR nuevo que referencia al anterior.
- **Estado.** Cada ADR tiene un estado: `Aceptado` (vigente), `Reemplazado` (una decisión posterior lo sustituye) o `Descartado` (se consideró y no se adoptó).
- **Estructura fija de tres secciones** (más alternativas):
  1. **Contexto** — el problema o situación que motivó la decisión.
  2. **Decisión** — qué se resolvió hacer, con el detalle técnico suficiente.
  3. **Alternativas consideradas** — qué otras opciones se evaluaron y por qué se descartaron.
  4. **Consecuencias** — qué implica la decisión para el desarrollo, la operación o el futuro del sistema.

> **Cómo leer un ADR:** empieza por el estado y el título; el título ya resume la decisión. Si necesitas el "por qué completo", lee Contexto y Alternativas; si necesitas saber qué tener en cuenta al tocar esa parte del sistema, lee Consecuencias.

Estos documentos complementan el modelo C4 ([c4.md](../c4.md)), el [modelo de datos](../data-model.md) y los [flujos de negocio](../business-flows.md).

## Índice de ADRs

Las fechas son estimadas según el orden de evolución del proyecto; para precisión exacta consulta el histórico de git y el código fuente.

| N.º | Título | Estado | Fecha (estimada) |
|---|---|---|---|
| [ADR-001](ADR-001-monolito-spring-boot.md) | Monolito Spring Boot que sirve API + SPA | Aceptado | 2024-Q2 |
| [ADR-002](ADR-002-jwt-cookie-httpOnly.md) | JWT en cookie `httpOnly` (sin localStorage) | Aceptado | 2024-Q3 |
| [ADR-003](ADR-003-csrf-cookie.md) | Protección CSRF con cookie + header | Aceptado | 2024-Q3 |
| [ADR-004](ADR-004-flyway-ddl-validate.md) | Flyway versionado + `ddl-auto=validate` en prod | Aceptado | 2024-Q3 |
| [ADR-005](ADR-005-postgresql-16.md) | PostgreSQL 16 (H2 solo test/dev) | Aceptado | 2024-Q3 |
| [ADR-006](ADR-006-jwt-revocation-denylist.md) | Revocación de JWT con lista de denegación | Aceptado | 2024-Q4 |
| [ADR-007](ADR-007-rate-limit-auth.md) | Rate limiting de autenticación en memoria | Aceptado | 2024-Q4 |
| [ADR-008](ADR-008-backup-postgres-cron.md) | Backup de PostgreSQL con cron + `pg_dump` | Aceptado | 2025-Q1 |
| [ADR-009](ADR-009-sin-redis.md) | Sin Redis (límites y revocación locales) | Aceptado | 2025-Q1 |
| [ADR-010](ADR-010-spa-vanilla-js.md) | SPA vanilla JS sin framework ni build | Aceptado | 2025-Q2 |
| [ADR-011](ADR-011-multa-plana.md) | Multa plana de devolución tardía | Aceptado | 2025-Q3 |

## Convenciones para añadir un ADR nuevo

1. Crea el archivo `ADR-XXX-nombre-corto.md` en este directorio, siendo `XXX` la siguiente posición libre.
2. Usa la estructura fija: título, estado, fecha, Contexto, Decisión, Alternativas consideradas y Consecuencias.
3. Añade la fila correspondiente en la tabla del índice y deja constancia de qué ADRs están relacionados si aplica.
4. Escribe en español profesional y neutro, coherente con estos documentos.