-- ============================================================
-- V6: Indexes on tokens_revocados denylist (performance only)
-- Optimizes TokenRevocadoRepository.deleteByEmail (email = ?)
-- and the hourly purgarExpirados (expira_en < now).
-- Non-destructive: CREATE INDEX only; no DROP / ALTER COLUMN.
-- Idempotent via IF NOT EXISTS (PostgreSQL 16 + H2 2.x).
-- ============================================================

CREATE INDEX IF NOT EXISTS idx_tokens_revocados_email_expira
    ON tokens_revocados (email, expira_en);

CREATE INDEX IF NOT EXISTS idx_tokens_revocados_expira
    ON tokens_revocados (expira_en);
