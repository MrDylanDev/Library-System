# ADR-007: Rate limiting de autenticación en memoria

- **Estado:** Aceptado
- **Fecha estimada:** 2024-Q4
- **Relacionados:** [ADR-009](ADR-009-sin-redis.md)

## Contexto

Los endpoints de autenticación (`login`, `register`, `forgot-password`) son el blanco natural de ataques de fuerza bruta o spam de registro. Sin límites, un atacante puede probar contraseñas o inundar de correos de recuperación sin coste alguno. Se necesita un control que frene este abuso sin infraestructura adicional.

## Decisión

Implementar un **rate limiter en memoria con ventana deslizante (sliding window)** en el filtro `AuthRateLimitFilter`, aplicado solo a las rutas sensibles:

- **Por IP: 60 peticiones/minuto** sobre `POST auth/login`, `register` y `forgot-password`.
- **Por email: 5 peticiones/15 minutos**, aplicado **solo a `login`** y **solo cuando el intento es un 401 real** (credenciales inválidas), para no castigar intentos legítimos fallidos por otra causa.
- **`X-Forwarded-For` se ignora salvo que se configure `trusted-proxies`** (vacío por defecto): sin configuración explícita, la IP real se toma de la conexión directa y no es falseable con cabeceras.
- La respuesta de exceso es **HTTP 429 en JSON**.
- El filtro usa un **body cacheado** para poder inspeccionar el email del payload sin consumir la petición.
- Sin Redis (ver [ADR-009](ADR-009-sin-redis.md)).

## Alternativas consideradas

- **Rate limiter distribuido con Redis:** resuelve el escalado horizontal pero requiere infraestructura; descartado por ahora (ver [ADR-009](ADR-009-sin-redis.md)).
- **Limitadores por librería externa (p. ej. Bucket4j):** útiles, pero la ventana deslizante en memoria integrada en el filtro cubre el requisito con menos dependencias.

## Consecuencias

- **Límites por instancia:** la ventana vive en la memoria del proceso; con varias instancias detrás de un balanceador los límites no son globales (consistente con [ADR-009](ADR-009-sin-redis.md)).
- **HTTP 429 JSON uniforme** para el cliente (SPA), con mensaje legible.
- **Filtro con body cacheado:** coste adicional de lectura del payload en las rutas protegidas, acotado a autenticación.
- **Seguridad de IP:** no confiar en `X-Forwarded-For` sin `trusted-proxies` evita que un cliente falsifique su IP y burle el límite.