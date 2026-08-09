-- ============================================================
-- V2: Cerrar divergencia de nullability
-- Las entidades tratan estos campos como obligatorios
-- (fechaDevolucion, estado, monto, etc.) pero el esquema baseline
-- los dejó NULLABLE. Los datos existentes ya no contienen NULLs,
-- así que es seguro endurecer las columnas.
-- ============================================================

-- préstamos: la fecha de devolución y el estado son obligatorios
ALTER TABLE prestamos ALTER COLUMN fecha_devolucion SET NOT NULL;
ALTER TABLE prestamos ALTER COLUMN estado SET NOT NULL;

-- multas: el monto y el estado son obligatorios
ALTER TABLE multas ALTER COLUMN monto SET NOT NULL;
ALTER TABLE multas ALTER COLUMN estado SET NOT NULL;

-- libros: el estado es obligatorio (default DISPONIBLE en la entidad)
ALTER TABLE libros ALTER COLUMN estado SET NOT NULL;

-- usuarios: el rol y el estado son obligatorios (defaults en la entidad)
ALTER TABLE usuarios ALTER COLUMN rol SET NOT NULL;
ALTER TABLE usuarios ALTER COLUMN estado SET NOT NULL;
