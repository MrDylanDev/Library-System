# Casos Críticos de Seguridad y Negocio

**Proyecto:** LibroMágico
**Alcance:** Escenarios críticos documentados, con resultado esperado y la capa de tests que los cubre.
**Referencia:** Matriz RBAC en [rbac.md](../security/rbac.md); metodología en [strategy.md](strategy.md).

Los casos se describen como **escenarios verificables**: ID, descripción, pasos, resultado esperado y capa que lo cubre. Si un cambio rompe uno de estos, el sistema ha roto un compromiso de seguridad o de negocio.

## 1. Matriz RBAC — acceso por rol

| Campo | Detalle |
|---|---|
| **ID** | `RBAC-1` |
| **Descripción** | Un usuario con rol menor no accede a los recursos administrativos. |
| **Pasos** | (a) Un `USER` (token válido) hace `GET /api/admin/dashboard`. (b) Un `LIBRARIAN` hace `PUT /api/admin/usuarios/{id}/rol`. (c) Un cliente anónimo hace `GET /api/prestamos`. |
| **Resultado esperado** | (a) `403 Forbidden`. (b) `403 Forbidden` (solo ADMIN cambia roles; LIBRARIAN sí ve dashboard). (c) `401 Unauthorized` (ruta autenticada). |
| **Capa que lo cubre** | Integración (`AuthorizationIntegrationTest`) + E2E `test_05`/`test_09` |

## 2. Ownership — un USER solo opera sobre sus propios datos

| Campo | Detalle |
|---|---|
| **ID** | `OWN-1` |
| **Descripción** | Un USER no puede prestar, devolver ni pagar multas de otro usuario, aun autenticado. |
| **Pasos** | Con token de `usuarioA`: (a) `POST /api/prestamos` con `usuarioId = usuarioB`. (b) `PUT /api/prestamos/{id}/devolucion` sobre un préstamo de `usuarioB`. (c) `PUT /api/multas/{id}/pagar` sobre una multa de `usuarioB`. |
| **Resultado esperado** | `403 Forbidden` en los tres casos (la capa de URL permite la ruta por rol, pero el servicio valida ownership y lo deniega). |
| **Capa que lo cubre** | Integración (`AuthorizationIntegrationTest`, casos de ownership) |

## 3. Préstamo — disponibilidad del libro

| Campo | Detalle |
|---|---|
| **ID** | `PR-1` |
| **Descripción** | No se presta un libro no disponible: PERDIDO, sin existencia, o copias agotadas. |
| **Pasos** | Para cada escenario: (a) libro con estado `PERDIDO` → `POST /api/prestamos`. (b) libro con `cantidad = 0` → préstamo. (c) estado "no disponible" → préstamo. (d) usuario con un préstamo **ACTIVO** del mismo libro → segundo préstamo. |
| **Resultado esperado** | `409 Conflict` en todos los casos (conflicto de estado / copias / duplicado de préstamo activo). |
| **Capa que lo cubre** | Unit (`PrestamoServiceUnitTest`), integración, E2E `test_07` (conflictos) |

## 4. Devolución tardía — multa

| Campo | Detalle |
|---|---|
| **ID** | `MUL-1` |
| **Descripción** | Una devolución fuera de plazo genera una multa de $10 en estado `PENDIENTE` y notifica por email. |
| **Pasos** | (a) Crear préstamo y forzar fecha de devolución vencida. (b) `PUT /api/prestamos/{id}/devolucion`. (c) Consultar multas del usuario. (d) Caso especial: devolver un préstamo **ATRASADO** no devuelto previamente. |
| **Resultado esperado** | (a–c) Multa `10.00` en estado `PENDIENTE` asociada al usuario + email de notificación. (d) La multa existente se **cierra** con la devolución. |
| **Capa que lo cubre** | Unit (`PrestamoServiceUnitTest`, lógica de multa), E2E `test_04` (pago de multas) |

## 5. Rate limiting de autenticación

| Campo | Detalle |
|---|---|
| **ID** | `RL-1` |
| **Descripción** | Se limita el abuso sobre el login sin bloquear intentos legítimos. |
| **Pasos** | (a) 5 intentos de login **fallidos** (credenciales incorrectas) con el mismo email en 15 min → `429`. (b) Más de 60 peticiones por minuto desde la misma IP → `429`. (c) Una petición CSRF inválida (falla antes de evaluar credenciales) **no** cuenta como intento fallido. |
| **Resultado esperado** | (a) `429 Too Many Requests` (JSON) por cuota de email. (b) `429` por cuota de IP. (c) La cuota de intentos fallidos **no se consume** (no es un `401` real). |
| **Capa que lo cubre** | Integración (rate limit: solo login, solo `401` reales contabilizados) |

## 6. Revocación de sesiones

| Campo | Detalle |
|---|---|
| **ID** | `REV-1` |
| **Descripción** | Un token deja de ser válido al hacer logout o al cambiar la contraseña. |
| **Pasos** | (a) Autenticarse, llamar `POST /api/auth/logout`, reutilizar el token previo. (b) Autenticarse, `reset-password`/cambio de contraseña, reutilizar el token previo. |
| **Resultado esperado** | (a) `401` (jti revocado en la denylist). (b) `401` (marcador `SHA-256(email)` invalida las sesiones previas). |
| **Capa que lo cubre** | Integración (revocación JWT); E2E `test_01` (logout/reset) |

## 7. Seguridad de headers y de exponer infraestructura

| Campo | Detalle |
|---|---|
| **ID** | `SEC-1` |
| **Descripción** | La respuesta trae CSP estricta y solo el mínimo de infraestructura queda expuesto. |
| **Pasos** | (a) `GET` a cualquier ruta (`/`, `/api/catalogo`, rutas autenticadas) e inspeccionar el header `Content-Security-Policy`. (b) `GET /actuator/health` y `GET /actuator/metrics` como anónimo. (c) Intentar `/h2-console` con perfil productivo. |
| **Resultado esperado** | (a) Header CSP estricto presente en todas las rutas (ver valores en [data-protection.md](../security/data-protection.md)). (b) `/actuator/health` → `200`; `/actuator/metrics` → `401`. (c) Inaccesible (`404`/denegado) — solo disponible con perfil `dev`. |
| **Capa que lo cubre** | Integración (`@SpringBootTest` + MockMvc, seguridad de headers); E2E `test_01` (la CSP estricta no rompe la SPA) |

## 8. Catálogo — búsqueda y paginación

| Campo | Detalle |
|---|---|
| **ID** | `CAT-1` |
| **Descripción** | El catálogo busca en múltiples campos y pagina resultados de forma determinista. |
| **Pasos** | (a) Búsqueda multi-campo (título/autor/etc.) → `GET /api/catalogo?q=...`. (b) Solicitar página 0 y siguiente; verificar `size = 20`. (c) Navegar con el paginador re-ejecutando la misma búsqueda. |
| **Resultado esperado** | (a) Resultados que coinciden en cualquiera de los campos. (b) **Pagina 0 base** con **20 ítems** por página (sin saltarse o duplicar resultados entre páginas). (c) El filtro se mantiene entre páginas. |
| **Capa que lo cubre** | Integración (paginación) + E2E `test_08` |

## 9. Borrado seguro de libro

| Campo | Detalle |
|---|---|
| **ID** | `LIB-1` |
| **Descripción** | No se puede eliminar un libro que tiene préstamos activos. |
| **Pasos** | (a) Crear libro y préstamo `ACTIVO`. (b) `DELETE /api/libros/{isbn}` (rol LIBRARIAN/ADMIN). |
| **Resultado esperado** | `409 Conflict` (integridad del historial de préstamos; el libro no se borra). |
| **Capa que lo cubre** | Unit + integración (servicio de libros) |

## 10. Recuperación de contraseña

| Campo | Detalle |
|---|---|
| **ID** | `PASS-1` |
| **Descripción** | El reset es seguro: no enumera usuarios y los tokens expiran. |
| **Pasos** | (a) `POST /api/auth/forgot-password` con un email existente y con uno inexistente. (b) `POST /api/auth/reset-password` con token vencido (mayor a 1 h). |
| **Resultado esperado** | (a) **Respuesta genérica e idéntica** en ambos casos (imposible saber si el email existe por la API). (b) **Error** de token inválido/expirado; no se resetea la contraseña. |
| **Capa que lo cubre** | Integración (flujo reset, expiración 1 h); E2E `test_01` |

---

## Checklist de regresión rápida

Antes de mergear un cambio que toque seguridad, autenticación, préstamos o multas, verifica:

- [ ] `RBAC-1` sigue verde (matriz integral en `AuthorizationIntegrationTest`)
- [ ] `OWN-1` sigue verde (ownership en servicios)
- [ ] `PR-1`, `MUL-1`: préstamos y multas en integración
- [ ] `RL-1`, `REV-1`, `SEC-1`, `PASS-1`: capa de seguridad intacta
- [ ] `./mvnw -Pcoverage verify` pasa el umbral ≥70% por instrucciones