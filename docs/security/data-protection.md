# Protección de Datos Personales

**Proyecto:** LibroMágico
**Alcance:** Datos personales que procesa el sistema, tratamiento, control de acceso, medidas técnicas y estado frente al Habeas Data (Ley 1581 de 2012, Colombia).

> **Aviso importante:** este documento describe las políticas y prácticas técnicas **del proyecto**, redactadas a partir de cómo está implementado el código. **No es asesoría legal** ni una política de privacidad formal para usuarios finales. Para un cumplimiento jurídicamente vinculante contrata un abogado o consultor especializado en protección de datos.

## Resumen

LibroMágico recolecta un **conjunto mínimo de datos personales** — identidad, contacto y credenciales de acceso — exclusivamente para operar una biblioteca: autenticar usuarios, prestar y devolver libros, y cobrar multas. Los datos se protegen con hashing de contraseñas, cookies `httpOnly`, CSRF, CSP estricta, limitación de tráfico y acceso restringido por rol y propiedad. El estado actual implementa parcialmente los principios de Habeas Data; las brechas para cumplimiento completo se listan al final.

## Datos personales que procesa el sistema

### Qué se recolecta y para qué

| Dato | Para qué se usa | Quién lo ve | Dónde se guarda |
|---|---|---|---|
| **Email** | Identificación única del usuario, login, envío de correos (confirmación de préstamo, multas, reset de contraseña) | USER (el suyo), LIBRARIAN/ADMIN (todos) | `usuario.email` + tokens de reset |
| **Nombre** | Identificar al usuario en préstamos, historial y administración | USER (el suyo), LIBRARIAN/ADMIN | `usuario.nombre` |
| **DNI** (8 dígitos) | Identificación nacional requerida por el préstamo | USER (el suyo), LIBRARIAN/ADMIN | `usuario.dni` |
| **Teléfono** | Contacto, comunicación sobre devoluciones o multas | USER (el suyo), LIBRARIAN/ADMIN | `usuario.telefono` |
| **Hash de contraseña** (BCrypt, sal) | Autenticación; nunca se guarda ni se recupera la contraseña en claro | Nadie (solo el hash, no legible) | `usuario.password` |
| **Rol y estado** | Autorización RBAC y habilitación de cuenta | USER (el suyo), LIBRARIAN/ADMIN (todos) | `usuario.rol`, `usuario.estado` |
| **Multas** (monto, estado) | Cobro por devolución tardía | El dueño (solo sus `PENDIENTE`), LIBRARIAN/ADMIN (todas) | tablas de multas |

**Fuera de alcance:** LibroMágico **no** almacena datos biométricos, financieros (tarjetas, cuentas), de salud, ni otra categoría sensible de la Ley 1581. Las multas solo registran un monto monetario y su estado; el pago no ingresa datos de medios de pago al sistema.

### Autenticación y sesión

- **JWT firmado (jjwt)** con claim `jti`, almacenado en **cookie `AuthToken`**: `httpOnly`, `SameSite=Strict`, `secure` en producción, expira a las **24 h**. No hay refresh token.
- **`XSRF-TOKEN`** en cookie no-`httpOnly` para el protocolo CSRF de la SPA (envío del header `X-XSRF-TOKEN`); el handler XOR está deshabilitado.
- Revocación por **denylist**: el `logout` revoca el `jti`; el cambio de contraseña invalida las sesiones previas vía un marcador `SHA-256(email)`; la lista se depura cada hora (job programado).

## Tratamiento de contraseñas

- **Nunca en claro:** solo se almacena el `hash BCrypt` (strength 10, sal automática). No existe flujo que devuelva o muestre la contraseña.
- **Reset por token:** `POST /api/auth/forgot-password` genera un token de reset que **expira a la 1 h**; se consume en `POST /api/auth/reset-password`. `forgot-password` devuelve una **respuesta genérica** (no revela si el email existe → evita enumeración de usuarios).
- Qué cambia al resetear: se actualiza el hash y se **revocan las sesiones previas** (marcador `SHA-256(email)` en la denylist).
- `BLOQUEADO` (enabled=false): el usuario no puede autenticarse aunque tenga credenciales válidas.

## Acceso y control

| Quién | Qué puede ver |
|---|---|
| **USER** | Solo **sus** datos: su perfil (`me`), su historial de préstamos (`/api/prestamos/usuarios/{id}` con su propio id), sus multas `PENDIENTE` (`/api/multas/mis-multas`). No ve DNI/teléfono de otros a través de la API. |
| **LIBRARIAN** | Todos los usuarios (`/api/usuarios/**`), todos los préstamos y todas las multas, dashboard. No modifica datos personales. |
| **ADMIN** | Todo lo de LIBRARIAN, más cambiar rol y estado (incluyendo bloquear cuentas). |
| **Anónimo** | Solo el catálogo público (`/api/catalogo`) y estáticos. No accede a datos personales. |

El control es de doble capa: **RBAC por URL** restringe la ruta, y **ownership en el servicio** garantiza que un USER solo toque filas propias (`403` en caso contrario). Ver [rbac.md](rbac.md) para la matriz completa.

## Medidas técnicas de protección

| Medida | Detalle |
|---|---|
| **TLS** | En producción se asume el despliegue **detrás de un reverse proxy** que termina TLS (instalación estándar de la [documentación de operaciones](../ops/README.md)). La cookie `AuthToken` es `secure` en prod, por lo que solo viaja por HTTPS. |
| **Cookie de sesión** | `AuthToken` con `httpOnly` (inaccesible a JS), `SameSite=Strict`, `secure` en prod, expiración 24 h. |
| **CSRF** | Habilitado: cookie `XSRF-TOKEN` (no-httpOnly, lectura por JS para armar el header) + header `X-XSRF-TOKEN` en las mutaciones de la SPA. |
| **CSP** | Estricta en la cadena principal: `default-src 'self'`; `script-src 'self'` (sin `unsafe-inline`/`eval`); `style-src 'self' 'unsafe-inline'` (el SPA genera estilos inline); `img-src 'self' data: https://covers.openlibrary.org`; `font-src 'self' data:`; `connect-src 'self'`; `object-src 'none'`; `base-uri 'self'`; `form-action 'self'`; `frame-ancestors 'self'`. La cadena Swagger (con autenticación, `@Order(1)`) usa una CSP laxa (`script-src 'unsafe-inline'` para los webjars). |
| **Rate limiting autenticación** | En memoria: **60 req/min por IP** (con reales `401` contabilizados) y **5 intentos fallidos por email cada 15 min** en login; respuesta `429` en JSON. El `X-Forwarded-For` se **ignora** si no hay `trusted-proxies` configurados (evita spoofing de IP). |
| **H2 console** | Exclusiva del perfil `dev` (`@Profile("dev")`) — inaccesible en producción. |
| **Denylist JWT** | Revocación de `jti` en logout y de sesiones en cambio de contraseña; purge horario. |
| **Logs** | Incluyen un **correlation ID** y no registran datos sensibles (ni contraseñas, ni tokens completos). |
| **Actuator** | `/actuator/health` y `/api/health` públicos (health check); `/actuator/**` restringido a ADMIN. `show-details=never`. |

## Cumplimiento Habeas Data (Ley 1581 de 2012, Colombia)

### Principios y cómo los aborda el proyecto hoy

| Principio (Ley 1581) | Cómo lo aborda LibroMágico hoy |
|---|---|
| **Legalidad** | Solo recolecta datos mínimos necesarios para la operación de una biblioteca (identidad, contacto, credenciales). No recolecta datos sensibles (biométricos, financieros, salud). |
| **Finalidad** | Cada dato tiene un propósito operativo explícito (autenticación, préstamo, cobro de multas, comunicación). No hay recolección con fines no relacionados. |
| **Libertad** | La creación de cuenta implica un **consentimiento implícito** del titular (no existe flujo de cuenta sin aceptar el tratamiento; queda por formalizar con manifestación expresa). |
| **Veracidad / Calidad** | Hay validaciones de formato (DNI de 8 dígitos, email, teléfono) que reducen datos erróneos; no existe aún un proceso de corrección por el titular. |
| **Transparencia** | Falta una **política de privacidad** publicada y accesible desde el registro/login; hoy no hay aviso de tratamiento previo a la recolección. |
| **Acceso y circulación restringida** | Implementado con RBAC por URL + ownership: un USER solo ve sus datos, los roles administrativos ven los que necesitan para operar. |
| **Seguridad** | Hashing BCrypt, cookie `httpOnly`, CSRF, CSP estricta, rate limiting, TLS en despliegue productivo. |
| **Confidencialidad** | Acceso por roles y por propiedad; logs sin datos sensibles; revocación de sesiones. |

### Brechas para el cumplimiento completo (no implementado)

Para cumplir integralmente la Ley 1581 un proyecto de este tipo necesita, entre otros:

1. **Política de privacidad formal** publicada y enlazada desde el registro y el login, con aviso de tratamiento previo y consentimiento **expreso** (no solo implícito).
2. **Proceso de solicitud del titular** (Habeas Data): mecanismos para que el usuario consulte, **corrija y suprima** sus datos (baja de cuenta + borrado), con plazos de respuesta definidos.
3. **Registro de base de datos ante la SIC** (Superintendencia de Industria y Comercio), si la operación lo requiere según la normativa vigente.
4. **Plazos de retención definidos** para datos personales (p. ej. regla de borrado de cuentas inactivas) y para datos transaccionales (historial de préstamos y multas).
5. **Designación de responsable del tratamiento** dentro del equipo que administra el despliegue.

> Estos ítems son decisiones de **producto y legales**, no cambios de código aislados; requieren alineación del equipo antes de implementarse.