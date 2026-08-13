# Especificación de Requisitos de Software (SRS) — LibroMágico

Este documento define los requisitos funcionales (RF) y no funcionales (RNF) del sistema de gestión bibliotecaria LibroMágico, conforme al sistema real implementado. Los RF se agrupan por módulo y cada uno incluye criterios de aceptación accionables; los RNF cubren seguridad, rendimiento, usabilidad, mantenibilidad, operación y observabilidad.

Referencia de contexto funcional: [project-charter.md](./project-charter.md).

## 1. Requisitos funcionales

Los identificadores siguen el formato `RF-XX` y son trazables al módulo correspondiente (ver [matriz de trazabilidad](#4-matriz-de-trazabilidad)).

### 1.1 Módulo: Autenticación (Auth)

| ID | Requisito | Criterios de aceptación |
|---|---|---|
| RF-01 | El sistema permitirá el registro de nuevos usuarios con correo y contraseña. | Se crea el usuario con rol `USER` por defecto. La contraseña se almacena hasheada con BCrypt. Fallar si el correo ya está registrado. |
| RF-02 | El sistema permitirá el inicio de sesión de usuarios registrados. | Credenciales válidas → emite JWT en cookie `httpOnly` y el usuario queda autenticado. Credenciales inválidas → rechaza con mensaje de error. |
| RF-03 | El sistema permitirá el cierre de sesión (logout) de un usuario autenticado. | El token del usuario se agrega a la denylist server-side y deja de ser válido. |
| RF-04 | El sistema permitirá la recuperación de contraseña por email mediante token. | El usuario solicita recuperación, recibe email con enlace/token y puede establecer una nueva contraseña. Al cambiar la contraseña, los tokens anteriores se revocan (denylist). |
| RF-05 | El sistema limitará la tasa de intentos de autenticación. | Máximo 60 peticiones/minuto por IP y 5 intentos por cada 15 minutos por email; superado el límite, responde con error de tasa. |
| RF-06 | El sistema protegerá las mutaciones contra CSRF. | Las peticiones que modifican estado (POST/PUT/DELETE) requieren token CSRF válido además del JWT. |

### 1.2 Módulo: Catálogo

| ID | Requisito | Criterios de aceptación |
|---|---|---|
| RF-10 | El sistema permitirá consultar el catálogo público de libros con búsqueda multi-campo y paginación. | La búsqueda filtra por texto en varios campos del libro y respeta la paginación (20 ítems por página como valor por defecto). El filtro se mantiene entre páginas. |
| RF-11 | El sistema mostrará los detalles de un libro. | Consulta por identificador/ISBN devuelve la información completa del libro y su estado de disponibilidad. |
| RF-12 | Solo `LIBRARIAN` y `ADMIN` podrán crear, editar y eliminar libros. | Un `USER` que intente crear/editar/eliminar un libro recibe error de permisos (403). |
| RF-13 | Solo `LIBRARIAN` y `ADMIN` podrán marcar un libro como Perdido. | El estado del libro pasa a `PERDIDO` y deja de estar disponible para préstamo. |

> **Nota sobre estados del libro:** el enum incluye `DISPONIBLE`, `PRESTADO`, `RESERVADO` y `PERDIDO`. `RESERVADO` es únicamente un valor legal del enum; **no existe flujo de reservas** en el sistema actual.

### 1.3 Módulo: Préstamos

| ID | Requisito | Criterios de aceptación |
|---|---|---|
| RF-20 | El sistema permitirá el préstamo de un libro disponible por un período de 15 días. | Si el libro está `DISPONIBLE`, se crea el préstamo con vencimiento a 15 días, el libro pasa a `PRESTADO` y el préstamo queda asociado al usuario solicitante. |
| RF-21 | Un usuario `USER` solo podrá ver y devolver sus propios préstamos. | La consulta de préstamos de un `USER` solo devuelve sus préstamos; devolver un préstamo ajeno → error de permisos. `LIBRARIAN`/`ADMIN` ven y gestionan todos. |
| RF-22 | El préstamo deberá cambiar de estado a Devuelto al devolverse. | La devolución cierra el préstamo y libera el libro (pasa a `DISPONIBLE`). |
| RF-23 | El sistema detectará automáticamente los préstamos atrasados. | Un scheduler horario marca como estado ATRASADO todo préstamo activo con vencimiento superado. |
| RF-24 | Una devolución tardía deberá generar una multa. | Al devolver después del vencimiento se crea una multa de monto plano **$10** con estado `PENDIENTE`. No se genera multa si la devolución es a tiempo. |

### 1.4 Módulo: Multas

| ID | Requisito | Criterios de aceptación |
|---|---|---|
| RF-30 | Las multas solo se crearán en devoluciones tardías, con monto plano de $10. | Refuerzo de RF-24: no existe multa por día; el monto es fijo y no hay otra vía de creación. Estados posibles: `PENDIENTE` / `PAGADO`. |
| RF-31 | El sistema permitirá al usuario pagar sus propias multas (autoservicio). | Un `USER` paga y ve solo sus multas; al pagar, el estado pasa a `PAGADO`. |
| RF-32 | El administrador podrá ver y gestionar las multas de todos los usuarios. | `LIBRARIAN`/`ADMIN` listan, filtran y marcan como pagadas las multas de cualquier usuario. |

### 1.5 Módulo: Administración

| ID | Requisito | Criterios de aceptación |
|---|---|---|
| RF-40 | Solo `ADMIN` podrá cambiar el rol o el estado de los usuarios. | `ADMIN` promueve/demueve roles (`USER`, `LIBRARIAN`, `ADMIN`) y habilita/deshabilita usuarios; `LIBRARIAN` y `USER` reciben error de permisos. |
| RF-41 | El sistema permitirá la gestión de usuarios. | Listar, buscar y actualizar usuarios; vista de detalle con sus préstamos y multas. |
| RF-42 | El sistema permitirá listar todos los préstamos y todas las multas con filtros. | Vistas administrativas de préstamos (con estado) y multas (con estado de pago), aplicando filtros de búsqueda. |

### 1.6 Módulo: Dashboard

| ID | Requisito | Criterios de aceptación |
|---|---|---|
| RF-50 | El panel administrativo mostrará estadísticas de la biblioteca. | El dashboard presenta las **9 estadísticas** de operación (por ejemplo: total de libros, préstamos activos, atrasados, multas pendientes, etc.), accesible para `LIBRARIAN`/`ADMIN`. |

## 2. Requisitos no funcionales

### Seguridad

| RNF | Descripción |
|---|---|
| RNF-01 | JWT emitido en cookie `httpOnly`; en producción con flag `Secure`. |
| RNF-02 | Contraseñas hasheadas con BCrypt. |
| RNF-03 | Política de Seguridad de Contenido (CSP) activa. |
| RNF-04 | Protección CSRF en mutaciones (ver RF-06). |
| RNF-05 | Rate limiting en endpoints de autenticación: 60 peticiones/min por IP y 5 por 15 min por email (ver RF-05). |
| RNF-06 | Revocación de tokens: denylist server-side al hacer logout y al cambiar contraseña. |
| RNF-07 | Cumplimiento de Habeas Data (tratamiento de datos personales, contexto colombiano). |
| RNF-08 | Autorización por rol en todos los endpoints: `USER`, `LIBRARIAN`, `ADMIN`. |

### Rendimiento

| RNF | Descripción |
|---|---|
| RNF-10 | Tasas de intentos de autenticación limitadas (ver RNF-05) para resistir fuerza bruta. |
| RNF-11 | El catálogo usa paginación con tamaño de página de 20 ítems para evitar cargas completas. |

### Usabilidad

| RNF | Descripción |
|---|---|
| RNF-20 | Interfaz SPA en español, con router hash propio en el cliente. |
| RNF-21 | Interfaz responsive, usable desde escritorio y dispositivos móviles. |
| RNF-22 | Navegación por roles: el menú se adapta a los permisos del usuario. |

### Mantenibilidad

| RNF | Descripción |
|---|---|
| RNF-30 | Cobertura de tests de backend ≥ 70 % de instrucciones (barrera JaCoCo). |
| RNF-31 | Suite E2E (Playwright + pytest) automatizada. |
| RNF-32 | CI en GitHub Actions que valida tests, E2E y backup/restore en cada push a `main` y PR. |
| RNF-33 | Migraciones de esquema gestionadas con Flyway. |

### Operación

| RNF | Descripción |
|---|---|
| RNF-40 | Backup automático diario de la base de datos (RPO 24 h). |
| RNF-41 | Restore con RTO < 30 minutos, respaldado por scripts probados en CI. |
| RNF-42 | Despliegue reproducible con Docker Compose (desarrollo y producción). |

### Observabilidad

| RNF | Descripción |
|---|---|
| RNF-50 | Spring Boot Actuator expone endpoints `health`, `info` y `metrics`. |
| RNF-51 | Endpoint de salud disponible para verificación (por ejemplo `/api/health`). |
| RNF-52 | IDs de correlación en logs para trazar peticiones de extremo a extremo. |

## 3. Datos de referencia (entorno dev)

Para exploración y pruebas locales, el perfil `dev` siembra 34 libros (9 reales + 25 de prueba) y 4 usuarios:

| Email | Rol | Notas |
|---|---|---|
| `admin@libromagico.com` | `ADMIN` | Acceso total |
| `librarian@libromagico.com` | `LIBRARIAN` | Gestión operativa |
| `usuario@libromagico.com` | `USER` | Usuario estándar |
| `moroso@libromagico.com` | `USER` | Con 2 multas pendientes de $10 |

## 4. Matriz de trazabilidad

| Requisito | Módulo |
|---|---|
| RF-01 a RF-06 | Autenticación |
| RF-10 a RF-13 | Catálogo |
| RF-20 a RF-24 | Préstamos |
| RF-30 a RF-32 | Multas |
| RF-40 a RF-42 | Administración |
| RF-50 | Dashboard |
| RNF-01 a RNF-08 | Seguridad |
| RNF-10 a RNF-11 | Rendimiento |
| RNF-20 a RNF-22 | Usabilidad |
| RNF-30 a RNF-33 | Mantenibilidad |
| RNF-40 a RNF-42 | Operación |
| RNF-50 a RNF-52 | Observabilidad |