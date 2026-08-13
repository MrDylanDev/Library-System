# ADR-004: Flyway versionado con `ddl-auto=validate` en producción

- **Estado:** Aceptado
- **Fecha estimada:** 2024-Q3
- **Relacionados:** [ADR-005](ADR-005-postgresql-16.md)

## Contexto

El esquema de la base de datos evoluciona con el código (nuevas entidades, restricciones, saneamiento de datos). Sin un mecanismo explícito, los cambios de esquema quedan dispersos en scripts ad-hoc o en la generación automática de JPA, y terminan divergiendo entre entornos. La producción no puede permitirse cambios de esquema fuera de control.

## Decisión

Gestionar el esquema con **Flyway, migraciones versionadas V1–V4**:

- `ddl-auto=validate` en **producción**: Hibernate valida que el modelo JPA coincida con el esquema migrado y **falla al arrancar si hay divergencia**. **Nunca** se usa `update` en producción.
- **Excepción — solo desarrollo:** el `docker-compose.yml` de desarrollo usa `ddl-auto=update` sobre Postgres; está delimitado al entorno local.
- **`schema.sql` está deprecado** y desactivado con `spring.sql.init.mode=never`; se eliminó como fuente de verdad del esquema.

Las migraciones se documentan en detalle en [data-model.md](../data-model.md).

## Alternativas consideradas

- **`ddl-auto=update` en producción:** genera cambios no versionados, destructivos y no reproducibles; rechazado.
- **Liquibase:** funcionalmente equivalente; Flyway se adoptó por simplicidad y por estar ya integrado en el stack.

## Consecuencias

- **Control del esquema:** toda evolución pasa por una migración versionada revisable en código; producción queda validada por `validate`.
- **V3 sanitiza backups antiguos:** la migración de saneamiento limpia datos heredados de copias de seguridad previas para que encajen con las restricciones nuevas.
- **`fecha_devolucion` NOT NULL:** la migración V2 aplica la restricción, garantizada por el servicio que siempre calcula la devolución a +15 días al crear el préstamo.
- **Arranque en rojo:** si el esquema real no coincide con las migraciones aplicadas y el modelo, la aplicación no arranca en producción — evidencia temprana de desajuste.