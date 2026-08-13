# Respuesta ante incidentes — LibroMágico

Procedimiento para responder a incidentes del entorno productivo: clasificación por severidad,
roles, runbook por tipo de fallo y pautas de comunicación y postmortem.

> Acciones ejecutables concretas para problemas tipo: ver la sección **Runbooks**. Para
> pérdida de datos, usa el procedimiento oficial que se referencia en el runbook 6.

## 1. Niveles de incidente

| Nivel | Definición | Ejemplos en LibroMágico |
|-------|-----------|--------------------------|
| **SEV1** | Caída total del servicio o pérdida de datos. Bloquea a todos los usuarios. | La app devuelve 503 en `/api/health`; la base PostgreSQL inaccesible; el host no responde; se necesita restore de datos. |
| **SEV2** | Degradación o funcionalidad rota para un subconjunto de usuarios o procesos. | Login/registro lentos o fallando; los emails de recuperación no llegan; un ítem del catálogo no se puede prestar por un error puntual; patrones de error elevados en un endpoint. |
| **SEV3** | Menor, cosmético o interno, sin impacto en usuarios. | Texto o estilo en la UI; log de `no-id` en alguna línea; una métrica del dashboard — que no afecta la operación normal. |

Regla de escalado: si un SEV2 no se resuelve en 30 minutos o afecta pagos/autenticación a
varios usuarios, se trata como SEV1 (o se comunica) para convocar a más gente.

## 2. Roles y responsabilidades

| Rol | Responsabilidad |
|-----|-----------------|
| **Líder de incidente** | Única persona que toma decisiones durante la mitigación y cambia el estado del incidente. Evita el "todos tocan todo". |
| **Investigador principal** | Sigue los logs (correlation IDs) y confirma root cause. Reporta al líder, no toma decisiones destructivas por su cuenta. |
| **Comunicador** | Mantiene informados a los interesados (usuarios afectados, otros administradores) sin interrumpir al equipo técnico. |
| **Alguacil de cambios** (si aplica) | Anota qué se cambió (env, comandos) para el postmortem. |

En un equipo pequeño, el líder y el investigador pueden ser la misma persona, pero la
comunicación debe delegarse o diferirse para no fragmentar la atención al root cause.

## 3. Runbooks por incidente tipo

### 3.1 API caída / 503 en health

1. `docker compose -f docker-compose.prod.yml -p libromagico ps` — ¿los 3 servicios están `Up`?
2. `docker compose ... logs app --tail=200` — busca `ERROR`/`Exception` o un fallo de arranque
   (`${VAR:?}` por variable faltante es lo más común si faltó `.env.prod`).
3. `curl -fsS http://localhost:8080/api/health` — si responde 200 pero algún endpoint da 500,
   no es una caída del stack: busca el `ERROR` en el log con su `correlationId`.
4. Si la app reinicia en bucle: lee los logs completos de bootstrap y comprueba que `db` está
   healthy (ver runbook 3.2).

### 3.2 Base de datos inaccesible

1. Salud: `curl /api/health` → sí `"db":"DOWN"`, la app responde 503.
2. `docker compose ps` — ¿está `db` corriendo? Sino: `docker compose up -d db`.
3. Logs: `docker compose logs db` — ¿disco lleno, auth fallando, contenedor se cae?
4. Volumen: `docker volume ls | grep postgres-data-prod` — si el volumen no existe, la base
   empezó vacía (no hay pérdida de datos: Flyway la recrea, pero los datos previos sí se
   perdieron → sigue el runbook 6 con un backup).
5. Verifica conectividad interna: `docker compose exec db pg_isready -U $DB_USER -d libromagico`.

### 3.3 Backups fallando

1. `docker logs libromagico-backup` y `tail -f backups/backup.log` — ¿hay `[backup] OK:` del
   último ciclo esperado (default 02:00)?
2. ¿El contenedor `backup` corre? ¿El cron tiene el schedule correcto (`BACKUP_SCHEDULE`)?
3. **Espacio en disco**: `df -h` en el host. El bind mount `./backups` crece con la retención
   (`find -mtime +N`); un disco lleno hace fallar `pg_dump` y `gzip`.
4. Verifica un dump: `ls -lh backups/libromagico_*.sql.gz` y que no estén corruptos
   (`gzip -t`).
5. Si el fallo persiste y no hay dumps recientes: alerta SEV2/SEV1 según cobertura de RPO
   (una nuit sin dump = RPO comprometido).

### 3.4 Degradación por rate limiting

Síntoma: usuarios ven respuestas 429 o bloqueos en login/registro/forgot-password.

1. ¿Ataque o configuración? Compara el volumen (p. ej. `docker compose logs app`) con la
   media histórica. Un **pico concentrado en una IP** sugiere fuerza bruta; picos **globales**
   con `no-id` o errores de CORS pueden ser un frontend mal configurado (origen equivocado en
   `CORS_ALLOWED_ORIGINS`).
2. Si es ataque: no rediseñes el límite aún; mitiga (bloquear IP en el firewall/host) y anota
   para endurecer el límite después.
3. Si es configuración: corrige el origen/listas y recarga `app` (reinicio limpio de los
   buckets de rate limit).

### 3.5 Sesiones invalidadas masivamente

Síntoma: todos los usuarios pierden el login a la vez.

- Causa más probable: **`JWT_SECRET` rotado** (se regeneró al recrear `.env.prod`). Todas las
  cookies JWT firmadas con el secreto anterior dejan de validar.
- Si el cambio fue accidental: restaura el `JWT_SECRET` anterior y reinicia `app`. Los tokens
  previos vuelven a validar.
- Si fue intencional (revocación global de seguridad): es el comportamiento deseado. Comunica
  a los usuarios que deben iniciar sesión de nuevo.
- La **denylist de revocación** (borrado bulk) no cubre un secreto rotado: la denylist solo
  bloquea tokens concretos revocados manualmente mientras fueron válidos.

### 3.6 Pérdida de datos → restore

> OJO: el restore **destruye los datos actuales** de la base. Solo proceder si se confirmó la
> pérdida y se decidió restaurar.

1. **PARAR**: detén el servicio `app` para congelar escrituras (los préstamos/multas nuevos
   durante el incidente se pierden; anota el punto temporal).
2. **Elegir el dump** más reciente anterior al momento de la pérdida: `ls -lh backups/`.
3. **Restaurar** siguiendo el procedimiento oficial paso a paso:
   **`docs/ops/backup-restore.md`** (recrear volumen → levantar solo `db` →
   `restore.sh` con `RESTORE_FILE=<dump>.sql.gz` → levantar `app`).
4. **Verificar**: salud de la app, count de libros/usuarios contra el dump, y que el backup
   cron vuelve a correr para no perder la protección RPO.
5. Si la pérdida es reciente (< 24 h) y el RPO lo permite, evalúa si conviene un dump manual
   con `docker exec libromagico-backup /usr/local/bin/backup.sh` antes.

## 4. Comunicación y postmortem

### Durante el incidente

- Líder decide el nivel y comunica al comunicador: qué se ve afectado, estimación, próxima
  actualización (p. ej. cada 30 min en SEV2).
- No se publican fixes parciales "a ver si". Cada cambio se registra (quién, cuándo, qué).

### Postmortem — documentar

Documenta todo incidente, aunque sea SEV2 (SEV1 siempre): timeline, root cause, acciones,
mejoras.

| Sección | Qué incluir |
|---------|-------------|
| **Timeline** | Hora de inicio/detección/resolución; cada intervención con su efecto. |
| **Root cause** | Causa raíz confirmada (no síntomas), con evidencia (logs, correlationId). |
| **Acciones** | Qué se cambió para mitigar y qué se cambió para evitar recurrencia. |
| **Mejoras** | Alertas/monitoreo a agregar, documentación a corregir, pruebas a cubrir. |

Cierre: actualiza la documentación afectada (este runbook, deployment, backup-restore) si el
incidente reveló un hueco, para que el próximo operador arranque con esas lecciones.

## Referencias

- Despliegue y verificación: `docs/ops/deployment.md`.
- Procedimiento oficial de backup/restore: `docs/ops/backup-restore.md`.
- Arquitectura de la infraestructura: `docs/ops/infrastructure.md`.