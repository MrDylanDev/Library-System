# ADR-002: JWT en cookie `httpOnly`

- **Estado:** Aceptado
- **Fecha estimada:** 2024-Q3
- **Relacionados:** [ADR-003](ADR-003-csrf-cookie.md), [ADR-006](ADR-006-jwt-revocation-denylist.md)

## Contexto

La aplicación es una SPA sin estado de sesión en el servidor. Se necesita autenticar al usuario y mantener su identificación entre peticiones de forma segura, teniendo en cuenta que el token viaja en un navegador y debe resistir los ataques típicos de aplicaciones web (fundamentalmente XSS y robo de credenciales).

## Decisión

Usar **JWT firmado** el token llevado en una **cookie `httpOnly`** llamada `AuthToken`:

- Librería **jjwt 0.12.5** para emitir y validar.
- Cookie con atributos:
  - `httpOnly` — el token no es legible desde JavaScript.
  - `SameSite=Strict` — no se envía con peticiones cross-site.
  - `secure` — solo sobre HTTPS, activo en producción.
- **Caducidad de 24 horas**.
- **Sin refresh tokens**: al expirar, el usuario vuelve a iniciar sesión.
- No se usa `localStorage`.

El `SecurityFilterChain` resuelve al usuario leyendo la cookie en el filtro JWT.

## Alternativas consideradas

- **`localStorage` + header `Authorization: Bearer`:** expone el token a cualquier XSS de la SPA; se descartó por riesgo de robo de sesión.
- **Sesiones server-side:** requieren mantener estado por usuario (y eventualmente un almacén compartido), contra el enfoque stateless elegido.
- **Refresh tokens (access + refresh):** mejoran la experiencia de renovación pero añaden complejidad notable (rotación, revocación) que no se justifica para una sesión de 24 h.

## Consecuencias

- **Protección XSS:** al ser `httpOnly`, un XSS no puede extraer el token.
- **Requiere defensa CSRF:** una cookie autenticada se envía sola en peticiones cross-site; ver [ADR-003](ADR-003-csrf-cookie.md).
- **Login cada 24 h:** sesiones de duración fija y sin renovación silenciosa.
- **Revocación mediante denylist:** al no haber refresh tokens y no poder invalidar JWT por sí solos, logout y cambio de contraseña usan la lista de denegación del [ADR-006](ADR-006-jwt-revocation-denylist.md).