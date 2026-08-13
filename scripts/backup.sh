#!/bin/sh
# === Backup PostgreSQL — LibroMágico ===
# Genera un dump plano SQL (pg_dump | gzip) en BACKUP_DIR y aplica retención.
# Configurable 100% por variables de entorno (ver docs/ops/backup-restore.md).
#
# Requeridas: PGHOST, PGUSER, PGPASSWORD, PGDATABASE.
# Opcionales: PGPORT (5432), BACKUP_DIR (/backups), BACKUP_RETENTION_DAYS (7),
#             BACKUP_PREFIX (libromagico).
set -eu
# pipefail: si pg_dump falla (ej: base caída), el pipeline falla y NO se
# escribe un dump corrupto que pase gzip -t. Guard por si el shell no lo soporta.
set -o pipefail 2>/dev/null || true

# --- Variables obligatorias: se falla con mensaje claro si alguna falta ---
for var in PGHOST PGUSER PGPASSWORD PGDATABASE; do
  eval "value=\"\${$var:-}\""
  if [ -z "$value" ]; then
    echo "[backup] ERROR: variable de entorno $var no definida." >&2
    echo "[backup] Defínala antes de ejecutar (ver docs/ops/backup-restore.md)." >&2
    exit 1
  fi
done

# --- Variables opcionales con default ---
PGPORT="${PGPORT:-5432}"
BACKUP_DIR="${BACKUP_DIR:-/backups}"
BACKUP_RETENTION_DAYS="${BACKUP_RETENTION_DAYS:-7}"
BACKUP_PREFIX="${BACKUP_PREFIX:-libromagico}"

# --- Crea el directorio de backups si no existe ---
mkdir -p "$BACKUP_DIR"

# --- Dump plano SQL comprimido (escritura atómica) ---
# Se escribe a FILE.tmp y se renombra SOLO si gzip -t lo valida, para que
# un dump fallido no deje un .sql.gz corrupto que parezca un backup válido.
FILE="$BACKUP_DIR/${BACKUP_PREFIX}_$(date +%Y%m%d_%H%M%S).sql.gz"
TMP_FILE="$FILE.tmp"
cleanup_tmp() { rm -f "$TMP_FILE"; }
trap cleanup_tmp EXIT

pg_dump -h "$PGHOST" -p "$PGPORT" -U "$PGUSER" -d "$PGDATABASE" | gzip > "$TMP_FILE"

# --- Verificación: el dump debe ser un gzip válido ---
if ! gzip -t "$TMP_FILE"; then
  echo "[backup] ERROR: el dump es inválido; se elimina $TMP_FILE" >&2
  exit 1
fi

mv "$TMP_FILE" "$FILE"
trap - EXIT

SIZE=$(du -h "$FILE" | cut -f1)
echo "[backup] OK: dump $FILE creado ($SIZE)."

# --- Retención: elimina dumps con más de BACKUP_RETENTION_DAYS días ---
old_files=$(find "$BACKUP_DIR" -name "${BACKUP_PREFIX}_*.sql.gz" -mtime +"$BACKUP_RETENTION_DAYS" 2>/dev/null || true)
if [ -n "$old_files" ]; then
  removed=$(printf '%s\n' "$old_files" | wc -l)
  find "$BACKUP_DIR" -name "${BACKUP_PREFIX}_*.sql.gz" -mtime +"$BACKUP_RETENTION_DAYS" -delete 2>/dev/null || true
  echo "[backup] Retención: $removed archivo(s) eliminado(s) (más de $BACKUP_RETENTION_DAYS día(s))."
else
  echo "[backup] Retención: sin archivos a eliminar."
fi