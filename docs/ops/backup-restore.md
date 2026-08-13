# Estrategia de Backup/Restore — Base PostgreSQL productiva

**Proyecto:** LibroMágico
**Estado:** Implementada como parte de la issue #60 (backup/restore de la base productiva).
**Alcance:** Dump por `pg_dump` (plano SQL comprimido), servicio `backup` con cron en el
stack productivo, retención en el host, restore guiado probado en CI.

---

## Objetivos de recuperación (RPO / RTO)

| Métrica | Objetivo | Cómo se cumple |
|---|---|---|
| **RPO** | 24 h | Dump diario a las 02:00 (configurable con `BACKUP_SCHEDULE`) |
| **RTO** | < 30 min | Restore guiado sin dependencias externas (solo `psql` + dump) |

> El RPO real puede ser menor que 24 h si se hacen backups manuales con mayor frecuencia
> (ver "Backup manual sin cron"). El RTO supone que el operador tiene el stack levantado o
> puede levantarlo con los comandos de este documento.

---

## Cómo funciona

- **Servicio `backup`** (`docker-compose.prod.yml`): basado en `postgres:16-alpine`, anexado
  a la red interna (sin puertos al host). Un `crond` ejecuta `scripts/backup.sh` dentro del
  contenedor según `BACKUP_SCHEDULE` (cron estándar de 5 campos, default `0 2 * * *`).
- **Decisión de diseño:** los dumps viven en un **directorio del host** vía bind mount
  (`${BACKUP_HOST_DIR:-./backups}` montado en `/backups` dentro del contenedor). El log del
  cron también aterriza en el host (`/backups/backup.log`).
- **`scripts/backup.sh`**: `pg_dump` plano SQL → `gzip` → archivo
  `libromagico_<YYYYMMDD_HHMMSS>.sql.gz`. Verifica el dump con `gzip -t` (si falla, lo borra
  y sale con error) y aplica retención: elimina dumps más viejos que
  `BACKUP_RETENTION_DAYS` (default 7) usando `find -mtime`.
- **`scripts/restore.sh`**: `gunzip -c <dump> | psql -v ON_ERROR_STOP=1 ...`. Asume una base
  de destino vacía/fresca.

---

## Uso en producción

### Levantar el stack

```bash
cp .env.prod.example .env.prod   # completar los secretos
docker compose --env-file .env.prod -f docker-compose.prod.yml -p libromagico up -d --build
```

### Ver el log del backup

```bash
docker logs libromagico-backup
tail -f backups/backup.log        # log de cada ejecución del cron (en el host)
```

### Listar los dumps en el host

```bash
ls -lh backups/libromagico_*.sql.gz
```

---

## Restore paso a paso

> OJO: el paso 2 **destruye los datos actuales** de la base productiva. Solo proceder si se
> está seguro de querer restaurar.

```bash
STACK="docker compose --env-file .env.prod -f docker-compose.prod.yml -p libromagico"

# 1) Parar la app (la base queda disponible para el restore)
$STACK stop app

# 2) Recrear el volumen de la base (OJO: destruye los datos actuales)
$STACK down
docker volume rm libromagico_postgres-data-prod

# 3) Levantar SOLO la base (crea un volumen nuevo y vacío, con la base "libromagico")
$STACK up -d db

# 4) Restaurar el dump elegido. restore.sh no está montado por defecto en el
#    contenedor que corre, así que se inyecta al vuelo y se ejecuta en un
#    contenedor desechable. El dump debe existir en $PWD/backups/ (bind mount → /backups).
RESTORE_FILE=/backups/libromagico_$(date +%Y%m%d).sql.gz   # <-- elegir el dump correcto
$STACK run --rm \
  -e RESTORE_FILE="$RESTORE_FILE" \
  -v "$PWD/scripts/restore.sh:/usr/local/bin/restore.sh:ro" \
  --entrypoint /usr/local/bin/restore.sh \
  backup

# 5) Levantar la app y verificar que el restore fue correcto
$STACK up -d app
curl -fsS http://localhost:8080/api/health
```

**Alternativa al paso 4 con `docker exec`** (requiere el contenedor `backup` corriendo):

```bash
docker cp scripts/restore.sh libromagico-backup:/usr/local/bin/restore.sh
docker exec -e RESTORE_FILE=/backups/<dump>.sql.gz libromagico-backup /usr/local/bin/restore.sh
```

> **Nota:** el comando del paso 4 usa `--entrypoint /usr/local/bin/restore.sh` de forma
> intencional: el entrypoint del servicio `backup` inicia el cron, así que un
> `docker compose ... run --rm backup ...` sin sobreescribir el entrypoint **no** ejecutaría
> el script. `docker compose run` hereda el entrypoint del servicio.

---

## Backup manual sin cron

```bash
STACK="docker compose --env-file .env.prod -f docker-compose.prod.yml -p libromagico"

# Opción A: contenedor desechable con el entrypoint del script (sobreescribe el cron)
$STACK run --rm --entrypoint /usr/local/bin/backup.sh backup

# Opción B: en el contenedor backup que ya corre (backup.sh está montado)
docker exec libromagico-backup /usr/local/bin/backup.sh
```

Para forzar retención distinta al ejecutar a mano:

```bash
docker exec -e BACKUP_RETENTION_DAYS=14 libromagico-backup /usr/local/bin/backup.sh
```

---

## Hardening recomendado (fuera de alcance)

- **Sincronización off-host** del directorio de backups (rsync/restic/S3): el dump almacenado
  solo en el host NO cubre la pérdida del servidor completo.
- **Cifrado** de los dumps en reposo (por ejemplo `gpg`/`age`) antes de la sincronización
  off-host, si la política de seguridad lo requiere.
- **Monitorización** del éxito del cron (alerta si no aparece `[backup] OK:` en `backup.log`
  o si el archivo de hoy no existe).

---

## Aviso importante

El backup vive en un **directorio del host** (`BACKUP_HOST_DIR`, default `./backups`). Ese
directorio **no sobrevive a la pérdida del host**. Protegerlo con permisos adecuados
(contiene un dump completo de la base) y sincronizarlo off-host (ver hardening), porque si
el host se pierde y no hay copia fuera, se pierde también el backup.