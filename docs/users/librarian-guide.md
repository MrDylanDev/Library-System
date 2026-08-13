# Guía del bibliotecario — LibroMágico

Manual para usuarios con rol **BIBLIOTECARIO** (LIBRARIAN): gestión de libros, control de
préstamos y consulta de multas y usuarios. Todo lo que no está en esta guía tampoco se puede
hacer con este rol.

## Acceso a la sección administrativa

Al iniciar sesión con un usuario bibliotecario, la barra de navegación muestra el enlace al
**panel administrativo** (`/admin`). Desde ahí accedes a las secciones disponibles:

- **Dashboard** — resumen general de la biblioteca.
- **Libros** — catálogo con acciones de gestión.
- **Préstamos** — listado y registro de devoluciones.
- **Multas** — consulta de multas generadas.
- **Usuarios** — consulta (solo lectura; ver limitaciones al final).

## Gestionar libros

### Crear un libro

1. Ve a **Libros → Nuevo libro** (`/admin/libros/nuevo`).
2. Completa título, autor, categoría, ISBN, editorial y demás datos.
3. Guarda. El libro aparece en el catálogo con estado **DISPONIBLE**.

### Editar un libro

1. Abre el detalle del libro y toca **Editar** (`/admin/libros/:isbn/editar`).
2. Modifica los campos que necesites. **El ISBN no se puede cambiar al editar**: es el
   identificador del libro.

### Eliminar un libro

En el detalle del libro, toca **Eliminar**.

> **Regla importante:** no se puede eliminar un libro con **préstamos activos**. Si el sistema
> lo impide, primero gestiona los préstamos pendientes de ese libro (espera la devolución o
> regístrala, o márcalo como perdido).

### Marcar un libro como PERDIDO

En el detalle del libro, toca **Marcar Perdido**. El libro queda con estado **PERDIDO** y ya no
se puede prestar. Útil cuando un ejemplar no se recupera. El proceso no se revierte desde esta
pantalla.

## Gestionar préstamos

### Ver préstamos

En **Préstamos** (`/admin/prestamos`) se listan los préstamos del sistema. Puedes **filtrarlos
por estado**:

| Estado | Significado |
|--------|-------------|
| **ACTIVO** | Prestado en plazo (falta tiempo para los 15 días). |
| **ATRASADO** | Pasó el plazo de 15 días sin devolución. Se generó multa de $10. |
| **DEVUELTO** | Ya devuelto y registrado. |

### Registrar una devolución

1. Localiza el préstamo (activo o atrasado).
2. Registra la devolución.

Si el préstamo estaba **atrasado**, el registro genera automáticamente la **multa de $10**
asociada al usuario.

## Ver multas y usuarios

- **Multas** (`/admin/multas`): lista las multas del sistema con su estado y monto. Como
  bibliotecario solo puedes **verlas**.
- **Usuarios** (`/admin/usuarios`): lista los usuarios registrados. Solo **consulta**:
  no puedes cambiar roles ni estados (bloquear/activar).

## El Dashboard

El panel principal muestra **9 tarjetas** con estadísticas de la biblioteca: volúmenes de
libros (total, disponibles, prestados, perdidos), préstamos (activos, atrasados, devueltos),
usuarios registrados y multas (pendientes, pagadas o el total recaudado/pendiente según la
versión). Usa estos números para decidir reposición de ejemplares, recordar morosos y detectar
pérdidas.

## Qué NO puede hacer un bibliotecario

| Acción | Quién la hace | Por qué |
|--------|---------------|---------|
| Cambiar el rol de un usuario (USER/LIBRARIAN/ADMIN) | Solo ADMIN | Escalada de privilegios. |
| Bloquear o reactivar usuarios | Solo ADMIN | Control de acceso a la comunidad. |
| Pagar multas | Solo ADMIN (el propio usuario paga desde **Mis multas**, y el admin por cualquier usuario) | Depuración de la cuenta. |
| Eliminar libros con préstamos activos | No permitido | Integridad del préstamo: rompería el historial. |

Si necesitas alguna de estas acciones, solicítala a un administrador.

## Acceso de prueba (entorno de desarrollo)

Con datos de ejemplo: `librarian@libromagico.com` / `librarian123` (rol BIBLIOTECARIO).

> Manual del administrador: `docs/users/admin-guide.md`. Guía del usuario final:
> `docs/users/user-guide.md`.