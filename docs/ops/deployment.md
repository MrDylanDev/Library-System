# Runbook de despliegue — LibroMágico

Este documento describe cómo desplegar el stack productivo de LibroMágico (Spring Boot +
PostgreSQL) en un servidor con Docker. El despliegue se hace exclusivamente con
`docker-compose.prod.yml`: la aplicación se empaqueta como imagen y el stack levanta los
servicios `app`, `db` y `backup`.

> Tiempo estimado para un despliegue correcto: 10-15 minutos. El stack no deja de servir si
> se levanta sobre una versión anterior (despliegue casi in-place).

## Requisitos previos

| Requisito | Detalle |
|-----------|---------|
| Docker + Compose v2 | `docker compose version` debe responder. Compose v2 es obligatorio. |
| Git | Para clonar el repositorio y obtener los `scripts/` y el `Dockerfile`. |
| Acceso a GitHub | Para clonar el repo (la imagen se construye localmente, no se usa registry). |
| Servidor con puerto libre | El `APP_PORT` (default `8080`) libre en el host. |
| Variables de entorno | Completar `.env.prod` (ver sección siguiente). |

No se requiere JDK ni Maven en el servidor: la imagen se construye en multi-stage dentro de
Docker.

## 1. Preparar las variables de entorno

Copia la plantilla y complétala:

```bash
cp .env.prod.example .env.prod
```

Variables obligatorias (la app **no arranca** si faltan — el compose las exige con `${VAR:?}`):

- `DB_USER` y `DB_PASS` — credenciales de la base PostgreSQL.
- `JWT_SECRET` — genera uno robusto:

  ```bash
  openssl rand -base64 48
  ```

  **No reutilices un JWT_SECRET de otro entorno** (ver "JWT_SECRET cambiado" en problemas
  comunes). Cámbialo solo cuando quieras invalidar todas las sesiones a la vez.
- `CORS_ALLOWED_ORIGINS` — **obligatoria**. Lista separada por comas de los orígenes del
  navegador que pueden llamar a la API. Ejemplo: `https://libromagico.com,https://www.libromagico.com`.
  Si la dejas vacía o mal puesta, el frontend no podrá hacer login ni ninguna llamada.
- `SMTP_HOST`, `SMTP_PORT` (default `587`), `SMTP_USER`, `SMTP_PASS`, `SMTP_FROM`
  (default `noreply@libromagico.com`) — para el envío de emails (registro/recuperación).
- `APP_BASE_URL` — URL pública de la aplicación, usada en los correos (enlaces de
  recuperación) y en la configuración de la cookie segura.

Variables opcionales (tienen default):

| Variable | Default | Función |
|----------|---------|---------|
| `APP_PORT` | `8080` | Puerto del host que se mapea al 8080 del contenedor `app`. |
| `DB_URL` | `jdbc:postgresql://db:5432/libromagico` | Conexión interna a la base. **No la cambies** salvo que sepas lo que haces: apunta al servicio `db` dentro de la red de Docker. |
| `BACKUP_SCHEDULE` | `0 2 * * *` | Cron de 5 campos del backup diario. |
| `BACKUP_RETENTION_DAYS` | `7` | Días que se conservan los dumps. |
| `BACKUP_HOST_DIR` | `./backups` | Directorio del host donde viven los dumps. |

## 2. Desplegar

```bash
# 1) Construir la imagen (multi-stage: compila con Maven y empaqueta el runtime)
docker compose -f docker-compose.prod.yml build

# 2) Levantar el stack (lee las variables de .env.prod)
docker compose --env-file .env.prod -f docker-compose.prod.yml -p libromagico up -d

# 3) Ver el estado de los servicios
docker compose --env-file .env.prod -f docker-compose.prod.yml -p libromagico ps
```

El servicio `app` espera a que `db` esté sano (healthcheck) antes de arrancar, así que la
primera subida tarda unos segundos más. Flyway aplica las migraciones de esquema al arrancar.

## 3. Verificar el despliegue

```bash
# 3a) Salud interna (app + DB)
curl -fsS http://localhost:8080/api/health
# {"status":"UP","db":"UP"}

# 3b) Actuator (health público)
curl -fsS http://localhost:8080/actuator/health

# 3c) Logs de arranque
docker compose -f docker-compose.prod.yml -p libromagico logs app
```

Verifica en los logs:

- Arranque limpio de Spring Boot sin `ERROR`/`Exception` en la fase de bootstrap.
- **Correlation IDs**: cada línea de log de una petición lleva `correlationId=...` (patrón
  `%X{correlationId:-no-id}`). Si todas las líneas dicen `no-id`, hay un problema de
  configuración de logging.
- La línea de Flyway: `Successfully applied N migrations to schema ...`.
- El backup programado: `tail -f backups/backup.log` (debe aparecer `[backup] OK:`).

## 4. Rollback

La app es **sin estado** (sesiones en cookie JWT, datos en PostgreSQL), por lo que volver a
una versión anterior es cambiar la imagen por la previa:

### Opción A — desplegar una imagen anterior de la misma fuente

```bash
# Reconstruir desde un tag git anterior (mismo código que produjo esa versión)
git checkout <tag-anterior>
docker compose --env-file .env.prod -f docker-compose.prod.yml -p libromagico up -d --build
git checkout main
```

### Opción B — solo recargar una imagen previa ya construida

```bash
# Si conservas la imagen previa en el host
docker compose --env-file .env.prod -f docker-compose.prod.yml -p libromagico up -d --force-recreate
# con la imagen taggeada previamente, p. ej. libromagico:0.1.0 → edita image: en el compose
```

> **OJO con Flyway.** Las migraciones son **solo hacia adelante**. Si la versión nueva aplicó
> migraciones de esquema, volver al código anterior **no revierte el esquema** y la app vieja
> puede fallar contra la base migrada. En ese caso el rollback real es: restaurar la base con
> un backup previo al despliegue (ver `docs/ops/backup-restore.md`) **y** desplegar la versión
> anterior. Si la versión nueva no tocó el esquema, basta con la Opción A/B.

## 5. Problemas comunes y fixes

| Síntoma | Causa probable | Solución |
|---------|----------------|----------|
| `/api/health` devuelve **503** (`"db":"DOWN"`) | La base está caída o no alcanzable. | Revisa `docker compose ps` (¿`db` sigue arriba?); logs de `db`; comprueba que el volumen `postgres-data-prod` existe. Si la DB está bien y sigue en DOWN, revisa que `DB_URL` apunte al servicio `db`. |
| **CORS**: el navegador bloquea las llamadas | `CORS_ALLOWED_ORIGINS` vacía o sin el origen exacto (protocolo + dominio + puerto). | Corrige la variable y reinicia `app`. Los orígenes deben coincidir exactamente. |
| **Todas las sesiones se invalidan de golpe** | `JWT_SECRET` cambió (p. ej. regenerado al recrear `.env.prod`). | Restaura el `JWT_SECRET` anterior. Si el cambio fue intencional (revocación global), es esperado. |
| `PORT` ya está en uso al hacer `up -d` | El puerto `APP_PORT` del host lo ocupa otro proceso. | Cambia `APP_PORT` en `.env.prod` o libera el puerto. |
| `app` reinicia en bucle al arrancar | Falta una variable obligatoria (`${VAR:?}` falla) o la DB no está lista. | `docker compose logs app` para ver qué variable falta; confirma que `db` está healthy. |
| El email de recuperación no llega | SMTP mal configurado o autenticación rechazada. | Revisa `SMTP_HOST/PORT/USER/PASS/FROM` y mira el log de `app` (las excepciones de envío salen ahí). El envío es asíncrono; busca en el log por el correlationId. |
| Backups no aparecen en `./backups` | Cron no ejecutó o `BACKUP_SCHEDULE` mal escrito. | Revisa `docker logs libromagico-backup` y `backups/backup.log`. |
| `metrics` de Actuator da 403 | `/actuator/metrics` exige rol ADMIN en prod. | Esperado. Usa credenciales de administrador. |

## 6. Checklist post-despliegue

- [ ] `docker compose ps` muestra `app`, `db` y `backup` con estado `Up`/`running`.
- [ ] `curl /api/health` responde `{"status":"UP","db":"UP"}`.
- [ ] `curl /actuator/health` responde `{"status":"UP"}`.
- [ ] Los logs de `app` no contienen errores de bootstrap y las líneas llevan `correlationId`.
- [ ] Flyway reportó `Successfully applied` sin errores.
- [ ] Login manual con un usuario dev (o real) desde el navegador funciona.
- [ ] `backups/backup.log` muestra al menos una ejecución `[backup] OK:` (o se lanza un
      backup manual con `docker exec libromagico-backup /usr/local/bin/backup.sh`).
- [ ] El correo de registro/recuperación se envía y el enlace funciona.
- [ ] (Opcional) Se hizo un restore de prueba en un entorno no productivo con el último dump.

## Referencias

- Estrategia y procedimiento de backup/restore: `docs/ops/backup-restore.md`.
- Arquitectura de infraestructura y CI/CD: `docs/ops/infrastructure.md`.
- Respuesta ante incidentes (incl. pérdida de datos): `docs/ops/incident-response.md`.
