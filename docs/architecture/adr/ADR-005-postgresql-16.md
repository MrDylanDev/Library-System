# ADR-005: PostgreSQL 16 como base de datos

- **Estado:** Aceptado
- **Fecha estimada:** 2024-Q3
- **Relacionados:** [ADR-004](ADR-004-flyway-ddl-validate.md), [ADR-008](ADR-008-backup-postgres-cron.md)

## Contexto

El sistema necesita una base de datos relacional que refleje el modelo de dominio (usuarios, libros, préstamos, multas, tokens revocados), con integridad referencial, restricciones de unicidad y soporte de transacciones para operaciones compuestas (por ejemplo, crear un préstamo y decrementar copias). Además, el entorno de pruebas debe ser ligero y determinista.

## Decisión

Usar **PostgreSQL 16** como base de datos de producción y en el `docker-compose` de desarrollo:

- Servicio `postgres:16-alpine` en `docker-compose.yml` y `docker-compose.prod.yml`.
- En producción la base se ejecuta **sin puertos expuestos al host**.
- **H2 en memoria** se usa únicamente en el perfil por defecto y en desarrollo sin Compose (principalmente tests de integración con Flyway y `@DataJpaTest`); nunca en producción.

El esquema se gestiona con Flyway (ver [ADR-004](ADR-004-flyway-ddl-validate.md)).

## Alternativas consideradas

- **MySQL:** maduro y muy difundido, pero PostgreSQL ofrece mejor cumplimiento de estándares, tipado (incluido `DECIMAL(10,2)` para multas) y una semántica de restricciones más fiel al modelo.
- **MariaDB:** alternativa a MySQL con las mismas limitaciones relativas para este proyecto.

## Consecuencias

- **Backups con `pg_dump`:** la estrategia de copia de seguridad del [ADR-008](ADR-008-backup-postgres-cron.md) usa las herramientas nativas de PostgreSQL.
- **Dialecto y tipos:** el mapeo Hibernate y las migraciones usan tipos de PostgreSQL (`DATE`, `TIMESTAMP`, `BIGSERIAL`, `DECIMAL`).
- **H2 como sustituto en tests:** el perfil de desarrollo sin Compose corre contra H2, que difiere en algunos matices de dialecto; los tests E2E reales (Playwright + Python) corren contra Postgres real para cubrir esa brecha.