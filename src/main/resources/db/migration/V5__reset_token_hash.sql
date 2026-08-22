-- ============================================================
-- V5: Guardar el hash del token de reset en vez del token en claro
-- El token crudo (UUID) solo viaja en el email; la columna persiste su
-- hash SHA-256. Si la base se ve comprometida, el token no es utilizable.
-- La columna existente se reutiliza (no hay valores válidos que conservar).
-- ============================================================

ALTER TABLE usuarios RENAME COLUMN reset_token TO reset_token_hash;
