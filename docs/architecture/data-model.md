# Modelo de datos de LibroMágico

Este documento describe el esquema relacional de LibroMágico: las cinco tablas, sus columnas, restricciones y relaciones, además de las migraciones de Flyway que lo construyen. Es el punto de referencia para entender `data-model` y las entidades JPA.

La fuente de verdad del esquema en producción son las **migraciones de Flyway V1–V4** con `ddl-auto=validate` (ver [ADR-004](adr/ADR-004-flyway-ddl-validate.md)). Los enums se almacenan como cadenas con restricción `CHECK` (ver nota al final).

---

## Diagrama entidad-relación

```mermaid
erDiagram
    usuarios {
        bigserial id PK "identificador"
        varchar(255) nombre "nombre completo"
        varchar(255) email UK "único, credencial"
        varchar(255) contrasena "hash BCrypt"
        varchar(255) dni UK "único, ^[0-9]{8}$"
        varchar(255) telefono "^\\+[0-9]{10,15}$"
        varchar(255) rol "USER | LIBRARIAN | ADMIN"
        varchar(255) estado "ACTIVO | BLOQUEADO"
        varchar(255) reset_token "token de recuperación"
        timestamp reset_token_expiry "caducidad del token"
    }

    libros {
        varchar(20) isbn PK "ISBN-10 o ISBN-13 validado"
        varchar(255) titulo "título"
        varchar(255) autor "autor"
        varchar(255) categoria "categoría"
        int anio_publicacion "año, no futuro"
        varchar(255) editorial "editorial"
        int copias_disponibles "copias prestables"
        varchar(255) estado "DISPONIBLE | PRESTADO | RESERVADO | PERDIDO"
    }

    prestamos {
        bigserial id PK "identificador"
        bigint usuario_id FK "prestatario → usuarios.id"
        varchar(20) libro_isbn FK "libro → libros.isbn"
        date fecha_prestamo "fecha de retiro"
        date fecha_devolucion "fecha prevista (+15 días), NOT NULL"
        date fecha_entrega_real "fecha real de devolución, nullable"
        varchar(255) estado "ACTIVO | DEVUELTO | ATRASADO"
    }

    multas {
        bigserial id PK "identificador"
        bigint prestamo_id FK "préstamo → prestamos.id"
        decimal(10,2) monto "importe de la multa"
        varchar(255) estado "PENDIENTE | PAGADO"
    }

    tokens_revocados {
        varchar(255) jti PK "identificador único del token"
        varchar(255) email "marca por cambio de contraseña"
        timestamp expira_en "cuándo puede purgarse"
    }

    usuarios ||--o{ prestamos : "realiza"
    libros ||--o{ prestamos : "se presta en"
    prestamos ||--o{ multas : "genera"
```

---

## Tablas

### `usuarios`

Personas registradas en el sistema (lectores, bibliotecarios, administradores).

| Columna | Tipo | Restricciones | Descripción |
|---|---|---|---|
| `id` | `BIGSERIAL` | `PK` | Identificador de usuario. |
| `nombre` | `VARCHAR(255)` | `NOT NULL` | Nombre completo. |
| `email` | `VARCHAR(255)` | `NOT NULL`, `UNIQUE` | Correo de acceso; identifica al usuario. |
| `contrasena` | `VARCHAR(255)` | `NOT NULL` | Hash **BCrypt** de la contraseña (nunca texto plano). |
| `dni` | `VARCHAR(255)` | `NOT NULL`, `UNIQUE`, formato `^[0-9]{8}$` | DNI de 8 dígitos, único. |
| `telefono` | `VARCHAR(255)` | formato `^\+[0-9]{10,15}$` | Teléfono internacional opcional. |
| `rol` | `VARCHAR(255)` | `CHECK`, `NOT NULL` | Rol: `USER`, `LIBRARIAN` o `ADMIN`. |
| `estado` | `VARCHAR(255)` | `CHECK`, `NOT NULL` | `ACTIVO` o `BLOQUEADO`. |
| `reset_token` | `VARCHAR(255)` | nullable | Token de recuperación de contraseña. |
| `reset_token_expiry` | `TIMESTAMP` | nullable | Caducidad del token de recuperación. |

### `libros`

Obras del catálogo, identificadas por su ISBN.

| Columna | Tipo | Restricciones | Descripción |
|---|---|---|---|
| `isbn` | `VARCHAR(20)` | `PK` | ISBN-10 o ISBN-13 validado (patrón `^(?:9[0-9]{9}[Xx]|…13)$`). |
| `titulo` | `VARCHAR(255)` | `NOT NULL` | Título de la obra. |
| `autor` | `VARCHAR(255)` | `NOT NULL` | Autor. |
| `categoria` | `VARCHAR(255)` | `NOT NULL` | Categoría o género. |
| `anio_publicacion` | `INT` | `NOT NULL`, no futuro (`@NotFutureYear`) | Año de publicación. |
| `editorial` | `VARCHAR(255)` | `NOT NULL` | Editorial. |
| `copias_disponibles` | `INT` | `NOT NULL` | Copias prestables en este momento. |
| `estado` | `VARCHAR(255)` | `CHECK`, `NOT NULL` | `DISPONIBLE`, `PRESTADO`, `RESERVADO` o `PERDIDO`. |

> **Nota sobre `RESERVADO`:** es un valor legal del enum, pero no existe flujo de reservas implementado; ninguna operación de negocio lleva un libro a `RESERVADO`.

### `prestamos`

Operaciones de préstamo de un usuario sobre un libro.

| Columna | Tipo | Restricciones | Descripción |
|---|---|---|---|
| `id` | `BIGSERIAL` | `PK` | Identificador del préstamo. |
| `usuario_id` | `BIGINT` | `FK → usuarios.id`, `NOT NULL` | Usuario prestatario. |
| `libro_isbn` | `VARCHAR(20)` | `FK → libros.isbn`, `NOT NULL` | Libro prestado. |
| `fecha_prestamo` | `DATE` | `NOT NULL` | Fecha de retiro del préstamo. |
| `fecha_devolucion` | `DATE` | `NOT NULL` | Fecha prevista: `fecha_prestamo + 15 días` (aplicada por la migración V2). |
| `fecha_entrega_real` | `DATE` | nullable | Fecha real de devolución; `NULL` mientras no se devuelve. |
| `estado` | `VARCHAR(255)` | `CHECK`, `NOT NULL` | `ACTIVO`, `DEVUELTO` o `ATRASADO`. |

### `multas`

Multas generadas por devolución tardía, asociadas a un préstamo.

| Columna | Tipo | Restricciones | Descripción |
|---|---|---|---|
| `id` | `BIGSERIAL` | `PK` | Identificador de la multa. |
| `prestamo_id` | `BIGINT` | `FK → prestamos.id`, `NOT NULL` | Préstamo que originó la multa. |
| `monto` | `DECIMAL(10,2)` | `NOT NULL` | Importe (multa plana configurable, ver [ADR-011](adr/ADR-011-multa-plana.md)). |
| `estado` | `VARCHAR(255)` | `CHECK`, `NOT NULL` | `PENDIENTE` o `PAGADO`. |

### `tokens_revocados`

Lista de denegación de JWT (ver [ADR-006](adr/ADR-006-jwt-revocation-denylist.md)). Tabla autónoma, sin relaciones.

| Columna | Tipo | Restricciones | Descripción |
|---|---|---|---|
| `jti` | `VARCHAR(255)` | `PK` / `UNIQUE` | Identificador único del token revocado por logout. |
| `email` | `VARCHAR(255)` | `NOT NULL` | Usuario asociado (también usado como marcador de cambio de contraseña). |
| `expira_en` | `TIMESTAMP` | `NOT NULL` | Momento en que la entrada puede purgarse. |

---

## Relaciones

| Relación | Cardinalidad | Detalle |
|---|---|---|
| `usuarios` — `prestamos` | 1 — * | Un usuario puede tener muchos préstamos; cada préstamo pertenece a un único usuario. |
| `libros` — `prestamos` | 1 — * | Un libro puede aparecer en muchos préstamos; cada préstamo referencia un único libro por ISBN. |
| `prestamos` — `multas` | 1 — * | Un préstamo puede generar una multa; cada multa pertenece a un único préstamo. |
| `tokens_revocados` | — | Sin relaciones; tabla auxiliar de seguridad. |

---

## Migraciones Flyway

| Migración | Contenido |
|---|---|
| **V1 — baseline** | Crea el esquema base: tablas `usuarios`, `libros`, `prestamos` y `multas` con sus columnas, `PK`, `FK`, unicidades y restricciones de formato. |
| **V2 — not null** | Endurece restricciones: aplica `NOT NULL` sobre `prestamos.fecha_devolucion` (la app siempre la calcula a +15 días al crear el préstamo). |
| **V3 — sanitize** | Saneamiento de datos: limpia y normaliza registros heredados de backups antiguos para que cumplan las restricciones del esquema vigente. |
| **V4 — tokens_revocados** | Crea la tabla `tokens_revocados` para la lista de denegación de JWT ([ADR-006](adr/ADR-006-jwt-revocation-denylist.md)). |

---

## Nota sobre los enums

Los enums (`rol`, `estado` de usuario, `estado` de libro/préstamo/multa) se almacenan como **cadenas** (`VARCHAR(255)`) con **restricción `CHECK`** a nivel de base de datos: los valores permitidos están definidos tanto en Java (enums JPA) como en el esquema. Esto garantiza que la base rechace valores inválidos aunque no pase por la capa de aplicación, y mantiene los valores legibles en consultas y backups.