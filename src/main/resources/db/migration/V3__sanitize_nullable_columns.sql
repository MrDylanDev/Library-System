-- ============================================================
-- V3: Saneamiento defensivo de NULLs
-- V2 endureció a NOT NULL las columnas críticas. Para entornos
-- cuyo esquema provenga del baseline V1 (sin V2, p. ej. una BD
-- restaurada desde un backup anterior), esta migración rellena
-- los NULLs residuales con los mismos defaults que usan las
-- entidades antes de volver a aplicar las restricciones.
-- Nota: en entornos donde V2 ya se aplicó, no hay filas afectadas.
-- ============================================================

-- Saneamiento: rellenar NULLs con los defaults de las entidades
UPDATE prestamos SET fecha_devolucion = CURRENT_DATE  WHERE fecha_devolucion IS NULL;
UPDATE prestamos SET estado          = 'ACTIVO'       WHERE estado IS NULL;
UPDATE multas    SET monto           = 0              WHERE monto IS NULL;
UPDATE multas    SET estado          = 'PENDIENTE'    WHERE estado IS NULL;
UPDATE libros    SET estado          = 'DISPONIBLE'   WHERE estado IS NULL;
UPDATE usuarios  SET rol             = 'USER'         WHERE rol IS NULL;
UPDATE usuarios  SET estado          = 'ACTIVO'       WHERE estado IS NULL;

-- Garantizar NOT NULL incluso si el esquema venía del baseline V1
ALTER TABLE prestamos ALTER COLUMN fecha_devolucion SET NOT NULL;
ALTER TABLE prestamos ALTER COLUMN estado SET NOT NULL;
ALTER TABLE multas ALTER COLUMN monto SET NOT NULL;
ALTER TABLE multas ALTER COLUMN estado SET NOT NULL;
ALTER TABLE libros ALTER COLUMN estado SET NOT NULL;
ALTER TABLE usuarios ALTER COLUMN rol SET NOT NULL;
ALTER TABLE usuarios ALTER COLUMN estado SET NOT NULL;
