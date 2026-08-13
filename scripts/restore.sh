#!/bin/sh
# === Restore PostgreSQL — LibroMágico ===
# Restaura un dump plano SQL comprimido (.sql.gz) con psql.
#
# Uso: RESTORE_FILE=/ruta/al/dump.sql.gz PGHOST=... PGUSER=... PGPASSWORD=... PGDATABASE=... scripts/restore.sh
#
# Requeridas: RESTORE_FILE, PGHOST, PGUSER, PGPASSWORD, PGDATABASE.
# Opcionales: PGPORT (5432).
#
# NOTA: se asume una base de destino vacía/fresca (creada con POSTGRES_DB=libromagico).
# Si la base ya tiene datos, recree el volumen primero (ver docs/ops/backup-restore.md,
# sección "Restore paso a paso").
set -eu
# pipefail: si gunzip falla (dump truncado/corrupto), el pipeline falla y NO
# se restaura un dump incompleto. Guard por si el shell no lo soporta.
set -o pipefail 2>/dev/null || true

# --- RESTORE_FILE obligatorio: uso claro si falta ---
if [ -z "${RESTORE_FILE:-}" ]; then
  echo "[restore] ERROR: RESTORE_FILE no definido." >&2
  echo "[restore] Uso: RESTORE_FILE=/ruta/al/dump.sql.gz scripts/restore.sh" >&2
  echo "[restore] Las variables PGHOST, PGUSER, PGPASSWORD y PGDATABASE también son obligatorias (PGPORT=5432 por defecto)." >&2
  exit 1
fi

if [ ! -f "$RESTORE_FILE" ]; then
  echo "[restore] ERROR: $RESTORE_FILE no existe." >&2
  exit 1
fi

case "$RESTORE_FILE" in
  *.sql.gz) ;;
  *)
    echo "[restore] ERROR: $RESTORE_FILE no termina en .sql.gz." >&2
    exit 1
    ;;
esac

# --- Variables obligatorias de conexión ---
for var in PGHOST PGUSER PGPASSWORD PGDATABASE; do
  eval "value=\"\${$var:-}\""
  if [ -z "$value" ]; then
    echo "[restore] ERROR: variable de entorno $var no definida." >&2
    echo "[restore] Defínala antes de ejecutar (ver docs/ops/backup-restore.md)." >&2
    exit 1
  fi
done

PGPORT="${PGPORT:-5432}"

# --- Restore con ON_ERROR_STOP: ante cualquier error, psql aborta ---
if ! gunzip -c "$RESTORE_FILE" | psql -v ON_ERROR_STOP=1 -h "$PGHOST" -p "$PGPORT" -U "$PGUSER" -d "$PGDATABASE"; then
  echo "[restore] ERROR: falló la restauración desde $RESTORE_FILE." >&2
  exit 1
fi

echo "[restore] OK: datos restaurados desde $RESTORE_FILE."