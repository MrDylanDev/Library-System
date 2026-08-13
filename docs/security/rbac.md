# Control de Acceso Basado en Roles (RBAC)

**Proyecto:** LibroMágico
**Alcance:** Matriz de roles, reglas exactas de autorización y guía de cambios.
**Modelo de roles:** `RolUsuario` = `USER`, `LIBRARIAN`, `ADMIN`. `EstadoUsuario` = `ACTIVO`, `BLOQUEADO`.

## Resumen

LibroMágico tiene tres roles — **USER**, **LIBRARIAN** y **ADMIN** — y define el acceso **100% por URL** en `SecurityConfig` (Spring Security). No se usa `@PreAuthorize` ni `@RolesAllowed` en ningún controller. Además del rol, los recursos de un usuario **solo son manipulables por su dueño** (control por *ownership*), salvo que el rol tenga privilegios administrativos.

**Lectura rápida:** si el rol no aparece en la celda de una acción, la respuesta es `403 Forbidden`; si la petición no está autenticada y el endpoint lo exige, la respuesta es `401 Unauthorized`.

## Matriz RBAC

Leyenda: **✓** permitido, **✗** denegado, **propio** permitido solo sobre los recursos del propio usuario (ownership), **—** anónimo (sin autenticación).

| Recurso / acción | Anónimo | USER | LIBRARIAN | ADMIN |
|---|---|---|---|---|
| `GET /api/auth/me` | ✗ (401) | ✓ | ✓ | ✓ |
| `POST /api/auth/login` · `register` · `logout` · `forgot-password` · `reset-password` | ✓ | ✓ | ✓ | ✓ |
| `GET /api/catalogo` (público) | ✓ | ✓ | ✓ | ✓ |
| `GET /api/health` · `GET /actuator/health` | ✓ | ✓ | ✓ | ✓ |
| `GET /actuator/**` (métricas, etc.) | ✗ | ✗ | ✗ | ✓ |
| `/` · `/index.html` · `/css/**` · `/js/**` (SPA) | ✓ | ✓ | ✓ | ✓ |
| `GET /api/libros/**` | ✗ | ✓ | ✓ | ✓ |
| `POST` / `PUT` / `DELETE` `/api/libros/**` | ✗ | ✗ | ✓ | ✓ |
| `PUT /api/admin/libros/{isbn}/perdido` (marcar PERDIDO) | ✗ | ✗ | ✓ | ✓ |
| `GET /api/usuarios/**` | ✗ | ✗ | ✓ | ✓ |
| `POST /api/prestamos` (crear préstamo) | ✗ | propio (solo su `usuarioId`) | ✓ | ✓ |
| `PUT /api/prestamos/{id}/devolucion` | ✗ | propio (solo sus préstamos) | ✓ | ✓ |
| `GET /api/prestamos/usuarios/{id}` (historial) | ✗ | propio (solo su `id`) | ✓ | ✓ |
| `GET /api/prestamos` (todos los préstamos) | ✗ | ✗ | ✓ | ✓ |
| `GET /api/multas/mis-multas` | ✗ | ✓ (solo multas propias y en estado `PENDIENTE`) | ✓ | ✓ |
| `PUT /api/multas/{id}/pagar` | ✗ | propio (solo sus multas) | ✓ | ✓ |
| `GET /api/admin/prestamos/**` | ✗ | ✗ | ✓ | ✓ |
| `GET /api/admin/multas` (todas) | ✗ | ✗ | ✓ | ✓ |
| `PUT /api/admin/multas/{id}/pagar` | ✗ | ✗ | ✗ | ✓ |
| `GET /api/admin/dashboard` | ✗ | ✗ | ✓ | ✓ |
| `PUT /api/admin/usuarios/{id}/rol` · `PUT /api/admin/usuarios/{id}/estado` | ✗ | ✗ | ✗ | ✓ |
| Resto de `/api/admin/**` | ✗ | ✗ | ✗ | ✓ |
| Cualquier otra ruta (`anyRequest`) | ✗ (401) | ✓ | ✓ | ✓ |

> **Nota de orden:** las reglas se evalúan en orden de especificidad en `SecurityConfig` (las más específicas primero). Los matchers mostrados arriba son los definidos explícitamente; lo que no matchea cae en `anyRequest` → `authenticated`.

## Roles detallados

### USER (usuario final / lector)

Es el rol que se asigna **siempre** al registrarse (nunca se auto-asigna admin). Puede:

- Consultar el **catálogo público** (`/api/catalogo`) y los **libros** en detalle.
- **Prestar y devolver libros** — pero únicamente **sobre su propia cuenta**: no puede crear un préstamo para otro `usuarioId`, ni devolver un préstamo de otro usuario.
- Ver **su historial** de préstamos (`GET /api/prestamos/usuarios/{id}` con su propio `id`).
- Ver y **pagar sus propias multas** — solo ve sus multas `PENDIENTE` en `/api/multas/mis-multas`; solo paga multas propias.
- Gestionar su sesión: login, logout, cambiar/resetear contraseña, ver `me`.

Un USER **no** puede: crear/modificar/eliminar libros, ver la lista completa de préstamos o multas, ver usuarios, ni acceder a `/api/admin/**`.

### LIBRARIAN (bibliotecario)

Todo lo de USER, más:

- **CRUD completo de libros** (crear, editar, eliminar) y **marcar un libro como PERDIDO**.
- Ver **todos los préstamos** (`GET /api/prestamos` y `GET /api/admin/prestamos/**`).
- Ver **todas las multas** y **pagar la multa de cualquier usuario**.
- Ver el **dashboard administrativo** (`GET /api/admin/dashboard`).
- **Ver la lista de usuarios** (`GET /api/usuarios/**`).

Un LIBRARIAN **no** puede: cambiar el rol o el estado de un usuario (`403`), ni acceder a las rutas de administración exclusivas de ADMIN (actuator, resto de `/api/admin/**`).

### ADMIN (administrador)

Todo lo de LIBRARIAN, más:

- **Cambiar el rol** (`PUT /api/admin/usuarios/{id}/rol`) y el **estado** (`PUT /api/admin/usuarios/{id}/estado`) de cualquier usuario — incluye **BLOQUEAR** cuentas.
- **Todas** las rutas `/api/admin/**` y `/actuator/**` (métricas, health con detalle según perfil).
- Pagar multas de cualquiera (privilegio compartido con LIBRARIAN en la matriz).

## Cómo se implementa

### 1. Autorización 100% basada en URL (`SecurityConfig`)

Toda la autorización vive en una sola cadena de filtros de Spring Security. Reglas exactas del mapeo (el orden refleja especificidad):

```
/api/auth/me                 → authenticated
/api/auth/**                 → permitAll            (login, register, logout, forgot/reset password)
/api/catalogo/**             → permitAll
/api/health                  → permitAll
/actuator/health             → permitAll
/actuator/**                 → hasRole(ADMIN)
/  /index.html  /css/**  /js/** → permitAll        (SPA estática)
GET /api/libros/**           → hasAnyRole(USER, LIBRARIAN, ADMIN)
/api/libros/**               → hasAnyRole(LIBRARIAN, ADMIN)      (cualquier método no-GET)
/api/usuarios/**             → hasAnyRole(LIBRARIAN, ADMIN)
GET /api/prestamos           → hasAnyRole(LIBRARIAN, ADMIN)
/api/prestamos/**            → hasAnyRole(USER, LIBRARIAN, ADMIN)
GET /api/admin/prestamos/**  → hasAnyRole(LIBRARIAN, ADMIN)
PUT /api/admin/libros/**     → hasAnyRole(LIBRARIAN, ADMIN)      (marcar PERDIDO)
GET /api/admin/dashboard     → hasAnyRole(LIBRARIAN, ADMIN)
/api/admin/**                → hasRole(ADMIN)
/api/multas/**               → authenticated
anyRequest                   → authenticated
```

Dos rutas quedan cubiertas por el mapeo combinado de forma intencional:

- `PUT /api/multas/{id}/pagar` → autenticado en la capa de URL; la restricción **por ownership** la aplica el servicio (ver abajo).
- `GET /api/multas/mis-multas` → autenticado en la capa de URL; el servicio lo filtra por el usuario autenticado y por estado `PENDIENTE`.

### 2. Control por ownership (además del rol)

La URL define *quién entra*; la capa de servicio define *sobre qué datos puede operar*. Para los recursos de un usuario, la regla es: **un USER solo actúa sobre sus propios datos**, y se devuelve `403 Forbidden` en caso contrario:

| Operación | Regla de ownership |
|---|---|
| `POST /api/prestamos` | el `usuarioId` del body debe ser el usuario autenticado → `403` si no |
| `PUT /api/prestamos/{id}/devolucion` | solo el dueño del préstamo → `403` si no |
| `GET /api/prestamos/usuarios/{id}` | solo el propio `id` → `403` si no |
| `PUT /api/multas/{id}/pagar` | solo el dueño de la multa → `403` si no |
| `GET /api/multas/mis-multas` | filtra por el usuario autenticado (no acepta `id` ajeno) |

Los roles administrativos **no** están sujetos a ownership: LIBRARIAN y ADMIN ven y operan sobre los recursos de cualquiera según la matriz.

### 3. Roles y estados de cuenta

- **Registro:** siempre asigna `USER`. No existe flujo que auto-asigne `LIBRARIAN` o `ADMIN` en el registro.
- **Estado `BLOQUEADO`:** mapea a `enabled=false` en Spring Security → **no puede autenticarse**. Un usuario bloqueado no obtiene token nuevo ni accede a recursos autenticados. Solo un ADMIN puede desbloquearlo (`PUT /api/admin/usuarios/{id}/estado`).
- **Cambio de rol/estado:** exclusivo de ADMIN (celda ✗ para LIBRARIAN en la matriz, `403`).

## Guía de cambios

Si agregas un endpoint o cambias el acceso a uno existente:

1. **Define el acceso en `SecurityConfig`**, en la cadena de autorización. La convención del repo es declarar el matcher por patrón de URL y rol, no anotar el controller.
2. **No uses `@PreAuthorize` / `@RolesAllowed`** en controllers. Es una decisión deliberada del proyecto: mantener **una sola fuente de verdad** para la autorización (fácil de auditar y probar con la matriz RBAC) y porque el ownership se valida en la capa de servicio con lógica, no con expresiones de SpEL dispersas.
3. **Actualiza la [matriz RBAC](#matriz-rbac)** de este documento en el mismo cambio.
4. **Agrega un caso crítico** en [critical-cases.md](../testing/critical-cases.md) y su test de integración en `AuthorizationIntegrationTest` (o la clase correspondiente), para que el nuevo acceso quede cubierto por la barrera de cobertura y por los casos documentados.
5. Si la regla implica ownership (un USER operando sobre recursos propios), documenta la condición de `403` en la capa de servicio correspondiente.
