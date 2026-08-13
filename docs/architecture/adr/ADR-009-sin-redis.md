# ADR-009: Sin Redis (límites y revocación locales)

- **Estado:** Aceptado
- **Fecha estimada:** 2025-Q1
- **Relacionados:** [ADR-006](ADR-006-jwt-revocation-denylist.md), [ADR-007](ADR-007-rate-limit-auth.md), [ADR-001](ADR-001-monolito-spring-boot.md)

## Contexto

El rate limiting de autenticación ([ADR-007](ADR-007-rate-limit-auth.md)) y la revocación de JWT ([ADR-006](ADR-006-jwt-revocation-denylist.md)) son servicios que se suelen resolver de forma natural con un almacén de datos en memoria compartido y de baja latencia, típicamente Redis. Sin embargo, LibroMágico se despliega como **una sola instancia** y no existe infraestructura de soporte adicional; incorporar Redis añade un servicio más que operar, monitorizar y respaldar.

## Decisión

**No introducir Redis.** Los dos mecanismos se implementan con los recursos ya existentes del propio proceso y de la base de datos:

- **Rate limiting** ([ADR-007](ADR-007-rate-limit-auth.md)): ventana deslizante **en memoria** del proceso (IP 60/min, email 5/15 min solo login y solo 401 reales).
- **Revocación de JWT** ([ADR-006](ADR-006-jwt-revocation-denylist.md)): **denylist en la propia base de datos** (tabla `tokens_revocados`) con purga horaria.

## Alternativas consideradas

- **Redis para rate limit y revocación distribuidos:** resuelve límites globales y revocación compartida entre instancias, pero añade un servicio, configuración, persistencia y monitorización adicionales.
- **Redis solo para rate limit:** mitiga la mitad del problema sin resolver la revocación distribuida y mantiene la complejidad; descartado por coherencia.

## Consecuencias

- **Límites por instancia:** la ventana de rate limit no es global entre varias instancias (aceptable con una sola instancia de despliegue).
- **Simplicidad de despliegue:** sigue habiendo exactamente dos piezas en runtime: la aplicación y PostgreSQL.
- **Revocación ligada a la BD:** la denylist comparte el ciclo de vida y la disponibilidad de la base de datos, algo natural dado que la validación JWT ya depende de ella.
- **Revisar si se escala horizontalmente:** si el despliegue pasa a varias instancias, reabrir esta decisión (rate limit global y denylist compartida son los primeros puntos que fallarían).