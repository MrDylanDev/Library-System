# Guía del administrador — LibroMágico

Manual para usuarios con rol **ADMINISTRADOR** (ADMIN). Incluye todo lo que puede hacer un
bibliotecario (ver `docs/users/librarian-guide.md`) más la gestión de usuarios y el pago de
multas de cualquier usuario.

## Resumen de capacidades

| Capacidad | Bibliotecario | Administrador |
|-----------|:---:|:---:|
| Crear, editar, eliminar y marcar libros como PERDIDO | ✅ | ✅ |
| Ver préstamos, filtrar por estado y registrar devoluciones | ✅ | ✅ |
| Ver multas y usuarios | ✅ | ✅ |
| Cambiar rol de usuario (USER/LIBRARIAN/ADMIN) | ❌ | ✅ |
| Bloquear / reactivar usuarios (BLOQUEADO) | ❌ | ✅ |
| Pagar multas de cualquier usuario | ❌ | ✅ |

## Gestionar libros

Acceso completo desde el panel administrativo: **Libros**. Puedes crear (`/admin/libros/nuevo`),
editar (**el ISBN no es editable**), eliminar (imposible si el libro tiene préstamos activos) y
marcar como **PERDIDO** desde el detalle. Detalles en la guía del bibliotecario.

## Gestionar préstamos

Listado y filtrado por estado en **Préstamos** (`/admin/prestamos`), con registro de
devoluciones. Un préstamo **atrasado** (más de 15 días) genera su multa de **$10** al
registrar la devolución.

## Gestionar usuarios

En **Usuarios** (`/admin/usuarios`) puedes cambiar, para cualquier cuenta:

- **Rol**: `USER`, `LIBRARIAN` o `ADMIN` — controla los permisos de la cuenta.
- **Estado**: `ACTIVO` o `BLOQUEADO` — un usuario **BLOQUEADO no puede iniciar sesión**
  hasta que lo reactives.

> **Guard anti-auto-exclusión:** el sistema impide que te cambies tu propio rol a uno inferior
> o que te bloquees a ti mismo desde esta pantalla. Así se evita quedar sin administrador
> por accidente. Esto es intencional: si necesitas cambiar tu propia cuenta, que lo haga otro
> administrador.

### Cambiar un rol

1. Localiza al usuario en **Usuarios**.
2. Selecciona el rol nuevo en la acción correspondiente.
3. Confirma el cambio. Los permisos del usuario cambian de inmediato (en su próxima login).

### Bloquear / activar

1. En **Usuarios**, elige bloquear (estado `BLOQUEADO`) o activar (estado `ACTIVO`).
2. Confirma. Un bloqueo impide el inicio de sesión desde ese momento.

## Pagar multas

En **Multas** (`/admin/multas`) ves todas las multas del sistema. Puedes **pagar la multa de
cualquier usuario** desde esta pantalla (el propio usuario también puede pagar sus multas desde
**Mis multas**). Útil para conciliar pagos recibidos en ventanilla o errores administrativos.

## El Dashboard

El panel muestra **9 tarjetas** con las estadísticas de la biblioteca (ver descripción en la
guía del bibliotecario). Como administrador úsalo además para:

- Detectar acumulación de multas pendientes.
- Identificar préstamos atrasados que requieran gestión.
- Revisar el volumen de libros PRESTADO vs DISPONIBLE para decidir compras.

## Prácticas recomendadas

- **Mantén al menos 1 administrador** disponible siempre. Si eres el único admin, revisa el
  guard anti-auto-exclusión antes de hacer cambios en tu cuenta y avisa a otro admin de
  confianza.
- **No compartas credenciales**: cada persona debe usar su propia cuenta. No prestes una
  cuenta ADMIN para tareas de bibliotecario.
- **Revisa el dashboard regularmente** (al menos una vez por semana) para detectar atrasos,
  pérdidas y multas sin gestionar.
- **Documenta los cambios de rol/estado** importantes: quién pidió el bloqueo y por qué, para
  trazabilidad posterior.

## Acceso de prueba (entorno de desarrollo)

Con datos de ejemplo: `admin@libromagico.com` / `admin123` (rol ADMINISTRADOR).

## Referencias

- Guía del bibliotecario: `docs/users/librarian-guide.md`.
- Guía del usuario final: `docs/users/user-guide.md`.
- Guía de usuario de todo el rol USER incluida en el manual del usuario final.