# ADR-003: Protección CSRF con cookie de token y header

- **Estado:** Aceptado
- **Fecha estimada:** 2024-Q3
- **Relacionados:** [ADR-002](ADR-002-jwt-cookie-httpOnly.md)

## Contexto

La autenticación se realiza mediante JWT en una cookie (ver [ADR-002](ADR-002-jwt-cookie-httpOnly.md)). Toda cookie autenticada se envía de forma automática por el navegador, lo que expone al sistema a **CSRF**: un sitio malicioso podría disparar peticiones de cambio de estado contra LibroMágico con la cookie del usuario. `SameSite=Strict` en la cookie mitiga el riesgo pero no es suficiente por sí solo (hay navegadores, extensiones o escenarios donde no se puede confiar ciegamente en el atributo).

## Decisión

Habilitar la protección CSRF de Spring Security integrada con la SPA:

- **`CookieCsrfTokenRepository`** que expone la cookie **`XSRF-TOKEN` con `httpOnly=false`** (la SPA debe poder leerla).
- El SPA reenvía ese valor en el header **`X-XSRF-TOKEN`** en toda petición que cambia estado.
- **Handler XOR deshabilitado**: se usa el handler adaptado a tokens no XOR. Spring Security 6.2 por defecto aplica un `XorCsrfTokenRequestAttributeHandler` (tokens con prefijo cifrado en la comparación), que **rechaza el valor crudo de la cookie que la SPA reenvía**; por eso se configura un handler que acepta el valor tal cual.
- **Cadena propia para Swagger** con `@Order(1)`, autenticada y con **CSP laxa** para poder servir los recursos webjar de la documentación.

## Alternativas consideradas

- **Desactivar CSRF por ser "stateless":** elimina una defensa en profundidad cuando la credencial viaja en cookie; se rechazó.
- **Confiar solo en `SameSite=Strict`:** reduce el vector pero no lo elimina en todos los escenarios; se mantuvo como refuerzo, no como única barrera.
- **Token CSRF en `localStorage` o meta:** añade superficie XSS; la cookie `httpOnly=false` limita el valor a un token descriptible y de corta vida por sesión.

## Consecuencias

- **Toda operación que cambia estado requiere header `X-XSRF-TOKEN`**: la SPA lo inyecta de forma centralizada en sus peticiones de escritura.
- **La cookie CSRF se regenera** según el ciclo de sesión de Spring Security; la SPA debe leer el valor vigente antes de cada escritura.
- **Complejidad de seguridad extra**: una cadena de filtros alternativa (Swagger) y la configuración específica del handler son un punto de mantenimiento que documentan estos ADRs.