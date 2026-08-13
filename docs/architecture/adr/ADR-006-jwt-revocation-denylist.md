# ADR-006: Revocación de JWT con lista de denegación (denylist)

- **Estado:** Aceptado
- **Fecha estimada:** 2024-Q4
- **Relacionados:** [ADR-002](ADR-002-jwt-cookie-httpOnly.md), [ADR-009](ADR-009-sin-redis.md)

## Contexto

Los JWT son autónomos y estadoless: una vez emitidos siguen siendo válidos hasta su expiración aunque el usuario cierre sesión o cambie su contraseña. Para que **logout** y **cambio de contraseña** sean efectivos de inmediato se necesita un mecanismo que invalide tokens ya emitidos.

## Decisión

Implementar una **lista de denegación (denylist)** respaldada en la propia base de datos, tabla `tokens_revocados`:

- **Por logout:** se registra el `jti` (identificador único del token) con su `email` y expiración (`expiraEn`), de modo que el filtro JWT rechace ese token mientras no haya expirado.
- **Por cambio de contraseña:** se persiste un **marcador `SHA-256(email)`** y se invalidan todos los tokens emitidos con `iat` (issued-at) anterior a ese marcador, revocando de golpe cualquier sesión previa del usuario.
- **Purga horaria (`@Scheduled`):** una tarea elimina periódicamente las entradas ya vencidas para que la tabla no crezca indefinidamente.
- **Borrado bulk con JPQL** para los marcadores de cambio de contraseña: el borrado masivo por filas evita la **race condition de UNIQUE** al invalidar varias sesiones del mismo email de forma concurrente.
- Sin Redis (ver [ADR-009](ADR-009-sin-redis.md)).

El filtro `JwtAuthenticationFilter` consulta la denylist al validar cada petición autenticada.

## Alternativas consideradas

- **Blacklist en Redis:** más rápida y natural para este patrón, pero requiere infraestructura adicional; se descartó (ver [ADR-009](ADR-009-sin-redis.md)).
- **Whitelist de sesiones:** mantener los tokens válidos activos en vez de los revocados invierte la lógica y complica el modelo; rechazado.
- **JWT sin revocación:** deja logout y cambio de contraseña sin efecto real hasta la expiración (24 h); inaceptable por seguridad.

## Consecuencias

- **Tabla adicional** `tokens_revocados` en el esquema (migración V4, ver [data-model.md](../data-model.md)).
- **Una consulta extra por petición autenticada** contra la denylist; con la purga horaria se mantiene acotada.
- **Sin Redis:** la revocación comparte el ciclo de vida de la base de datos; si se escala horizontalmente, la denylist debe ser compartida (revisar [ADR-009](ADR-009-sin-redis.md)).