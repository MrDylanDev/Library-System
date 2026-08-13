# ADR-008: Backup de PostgreSQL con cron y `pg_dump`

- **Estado:** Aceptado
- **Fecha estimada:** 2025-Q1
- **Relacionados:** [ADR-005](ADR-005-postgresql-16.md)

## Contexto

Los datos del catálogo, los usuarios y el historial de préstamos y multas son irremplazables. El objetivo operativo es:

- **RPO de 24 h**: como máximo se puede perder un día de datos.
- **RTO inferior a 30 min**: restaurar un entorno funcional en menos de media hora.

Ningún despliegue de producción es aceptable sin una copia de seguridad automatizada, verificable y fuera del contenedor de la aplicación.

## Decisión

Añadir un **servicio dedicado de backup** en `docker-compose.prod.yml`, separado del contenedor de la aplicación:

- Imagen **`postgres:16-alpine`** con **`crond`** ejecutando `scripts/backup.sh` según la variable `BACKUP_SCHEDULE`.
- **`scripts/backup.sh`**:
  - vuelca la base con **`pg_dump | gzip`**;
  - escribe de forma **atómica** (volcado a archivo temporal → verificación con `gzip -t` → `mv` al destino definitivo), de modo que nunca queda un backup truncado como si fuera válido;
  - aplica **retención** con `find -mtime` según `BACKUP_RETENTION_DAYS` (por defecto 7 días);
  - escribe en el host vía **bind mount** `./backups:/backups` (configurado con `BACKUP_HOST_DIR`), fuera del contenedor y del volumen de datos.
- **`scripts/restore.sh`**: descomprime con `gunzip` y carga con `psql ... ON_ERROR_STOP=1`, abortando ante cualquier error y garantizando integridad.
- La base de producción corre **sin puertos expuestos al host**; el contenedor de backup se comunica con ella por la red interna.

## Alternativas consideradas

- **`pg_dump` manual:** no cumple el RPO y depende de la disciplina humana; rechazado.
- **Backup por volumen (copia de archivos de datos):** rompe la atomicidad y no es fiable con PostgreSQL en caliente; rechazado.
- **Servicio externo de backup (manejado):** válido, pero introduce un proveedor y coste adicionales para una instalación que ya está contenerizada.

## Consecuencias

- **Backup fuera del contenedor de la app:** la app no depende del agente de backup y un fallo del primero no rompe la segunda.
- **Volúmenes separados:** datos de PostgreSQL y destino de backups no comparten almacenamiento.
- **Verificación en CI:** el pipeline de GitHub Actions ejercita el ciclo **backup → restore** para demostrar que los scripts funcionan (ver [c4.md](../c4.md)).
- **V3 sanitiza backups antiguos:** si un backup viejo se restaura sobre un esquema nuevo, la migración de saneamiento de Flyway limpia los datos heredados (ver [ADR-004](ADR-004-flyway-ddl-validate.md)).