# Runbook de rotación de JWT_SECRET — LibroMágico

> Last verified: 2026-09-18 · versioned alongside code

Este documento describe cómo rotar el secreto de firma JWT (`JWT_SECRET`) en el
stack productivo de LibroMágico. La rotación es **hard cutover**: cambiar el
secreto invalida todas las sesiones existentes de golpe y todos los usuarios
deben volver a hacer login. No existe rotación gradual ni automática.

## 1. Cuándo rotar / impacto

Rota el secreto cuando:

- El secreto actual pudo haberse expuesto (leak en logs, `.env.prod`
  compartido por error, acceso no autorizado al servidor).
- Un miembro del equipo con acceso al secreto deja el proyecto.
- Lo exige una política periódica de rotación de credenciales.

**Impacto (hard cutover):** en cuanto el `app` redeployado arranca con el nuevo
`JWT_SECRET`, todos los tokens firmados con el secreto anterior son
rechazados. Todas las sesiones se invalidan inmediatamente, incluida la del
operador. Es esperado: avisa a los usuarios que deberán re-autenticarse.

## 2. Procedimiento

Requisitos previos:

| Requisito | Detalle |
|-----------|---------|
| Acceso SSH al servidor productivo | Para editar `.env.prod` y correr `docker compose`. |
| `openssl` disponible | Para generar el secreto (`openssl version` debe responder). |
| Ventana de mantenimiento comunicada | La rotación cierra todas las sesiones activas. |

Pasos:

```bash
# 1) Generar el nuevo secreto (mínimo 32 bytes / 256 bits)
openssl rand -base64 48

# 2) Guardar el secreto anterior por si hay que revertir
cp .env.prod .env.prod.bak-$(date +%F)

# 3) Reemplazar JWT_SECRET en .env.prod con el valor generado
#    (edita el archivo; no lo pegues en el historial del shell)
```

```bash
# 4) Redeployar el stack productivo con el nuevo secreto
docker compose --env-file .env.prod -f docker-compose.prod.yml -p libromagico up -d --build

# 5) Verificar el arranque
docker compose --env-file .env.prod -f docker-compose.prod.yml -p libromagico logs app
curl -fsS http://localhost:8080/api/health
# {"status":"UP","db":"UP"}
```

```bash
# 6) Verificar que los tokens viejos ya no valen y los nuevos sí:
#    - Pide a un usuario de prueba que haga login y navegue (debe funcionar).
#    - Una cookie AuthToken emitida antes de la rotación debe devolver 401.
```

## 3. Por qué hard cutover

LibroMágico usa **un solo secreto + guardia de arranque + denylist**
(`tokens_revocados`), que es suficiente para una aplicación única. La rotación
gradual con `kid` (key id) o doble secreto permitiría cero downtime, pero suma
complejidad (cabeceras `kid`, ventana de aceptación dual, coordinación de
deploy) sin beneficio proporcional a esta escala. Por eso `kid`/dual-key está
fuera de alcance (G-03): la rotación documentada aquí es corte directo,
asumido y comunicado.

## 4. Triple capa de protección + verificación

La producción rechaza un `JWT_SECRET` ausente o débil en tres capas,
de la más temprana a la más tardía:

| Capa | Dónde | Qué hace |
|------|-------|----------|
| 1. Compose | `docker-compose.prod.yml` (`JWT_SECRET: ${JWT_SECRET:?}` — sintaxis Compose que exige la variable) | Falla rápido si la variable falta en `.env.prod`. |
| 2. Properties | `application-prod.properties` (`jwt.secret=${JWT_SECRET}` — placeholder Spring requerido, sin default) | Spring no resuelve el placeholder y el contexto no arranca (`Could not resolve placeholder 'JWT_SECRET'`). |
| 3. Java | `JwtTokenProvider.requireProductionSecret` | Rechaza secreto en blanco, el valor dev por defecto y secretos de menos de 32 bytes con `IllegalStateException`. |

Verificación de la guardia (el arranque debe fallar sin secreto):

```bash
# 1) Guardas estáticas + guardia Java (no levantan Spring prod ni resuelven
# el placeholder; JwtTokenProviderSecretValidationTest mockea Environment)
./mvnw test -Dtest=JwtSecretHygieneFileContentTest,JwtTokenProviderSecretValidationTest

# 2) Fail-fast real de Spring/Compose (requiere Docker; sin arrancar prod)
docker compose --env-file /dev/null -f docker-compose.prod.yml -p libromagico config > /dev/null
# sin JWT_SECRET en el entorno: ERROR por variable requerida (${JWT_SECRET:?} capa 1)
# con JWT_SECRET exportado: config resuelve sin error; el placeholder
# requerido jwt.secret=${JWT_SECRET} (capa 2) y requireProductionSecret (capa 3)
# actúan al arrancar el app con perfil prod
```

## 5. Rollback

Si la rotación falla (secreto nuevo demasiado corto, error de deploy):

```bash
# 1) Restaurar el secreto anterior
cp .env.prod.bak-<fecha> .env.prod

# 2) Redeployar con el secreto restaurado
docker compose --env-file .env.prod -f docker-compose.prod.yml -p libromagico up -d --build
```

Rollback de los índices V6 (solo si hay que revertir el cambio de esquema;
es no destructivo y neutral para la correctitud, así que normalmente no hace
falta):

```sql
DROP INDEX IF EXISTS idx_tokens_revocados_email_expira;
DROP INDEX IF EXISTS idx_tokens_revocados_expira;
```

Rollback completo del cambio `jwt-secret-hygiene`: `git revert` de los
commits del cambio BORRA `V6__tokens_revocados_indexes.sql` del árbol
(revert no preserva archivos agregados). Si la BD ya aplicó V6, desplegar
ese árbol rompe la validación de Flyway (migración aplicada sin archivo
resuelto). Por eso, tras el revert hay que retener/re-agregar el archivo —
p. ej. `git checkout <sha-previo-al-revert> -- src/main/resources/db/migration/V6__tokens_revocados_indexes.sql` —
o restaurar un backup de la BD previo a V6 (ver `docs/ops/deployment.md`).
El rollback de esquema solo es posible restaurando un backup de la BD
previo a V6; un revert solo de código sin el `DROP INDEX` deja índices
extra inofensivos.

## 6. Problemas comunes y fixes

| Síntoma | Causa probable | Solución |
|---------|----------------|----------|
| `app` no arranca: `Could not resolve placeholder 'JWT_SECRET'` | `JWT_SECRET` vacío o ausente en `.env.prod`. | Completa `JWT_SECRET` en `.env.prod` y redeploya. |
| `app` no arranca: `JWT_SECRET es la secret por defecto de desarrollo` | Se copió el valor dev (`LibroMagico...`) a producción. | Genera uno nuevo con `openssl rand -base64 48`. |
| `app` no arranca: `JWT_SECRET es demasiado corta` | Secreto de menos de 32 bytes. | Genera uno nuevo con `openssl rand -base64 48` (48 bytes en base64 sobra). |
| Todas las sesiones caen a 401 tras el deploy | Rotación aplicada (hard cutover). | Esperado: pide re-login a los usuarios. Si no fue intencional, restaura el secreto anterior (rollback). |
| Login funciona pero viejas cookies siguen válidas | El deploy no tomó el secreto nuevo (imagen vieja). | Redeploya con `--build` y confirma el valor efectivo en el contenedor. |

## 7. Checklist post-rotación

- [ ] `docker compose --env-file .env.prod -f docker-compose.prod.yml -p libromagico ps` muestra `app`, `db` y `backup` en `Up`/`running`.
- [ ] `curl /api/health` responde `{"status":"UP","db":"UP"}`.
- [ ] Login manual desde el navegador funciona y emite una cookie nueva.
- [ ] Una cookie emitida antes de la rotación devuelve `401` (cutover efectivo).
- [ ] Se borró el backup `.env.prod.bak-<fecha>` o se guardó en un lugar seguro.
- [ ] Se avisó a los usuarios que debieron re-autenticarse.
