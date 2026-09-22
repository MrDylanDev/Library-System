package com.libromagico.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * File-content assertions for the {@code jwt-secret-hygiene} change.
 *
 * <p>These tests verify the static hardening artifacts without starting a
 * Spring context: prod fail-fast placeholder, compose hygiene, V6 migration
 * existence/content, runbook discoverability, and dev-only documentation.
 * Runtime guard behavior is covered by
 * {@code JwtTokenProviderSecretValidationTest} and
 * {@code TokenRevocationIntegrationTest}.
 */
class JwtSecretHygieneFileContentTest {

    private static final String DEV_FALLBACK =
            "LibroMagico2024SecretKeyParaFirmarJWTMinimo256Bits!!";

    private static Path repoRoot() {
        return Path.of(System.getProperty("user.dir"));
    }

    private static String read(String relative) throws IOException {
        return Files.readString(repoRoot().resolve(relative));
    }

    @Test
    @DisplayName("prod properties fails fast without JWT_SECRET")
    void prodProperties_failsFastWithoutJwtSecret() throws IOException {
        String content = read("src/main/resources/application-prod.properties");
        String jwtLine = content.lines()
                .filter(line -> line.stripLeading().startsWith("jwt.secret="))
                .findFirst()
                .orElseThrow(() -> new AssertionError(
                        "application-prod.properties must define a jwt.secret line"));
        assertTrue(jwtLine.contains("jwt.secret=${JWT_SECRET}"),
                "application-prod.properties must define jwt.secret with required ${JWT_SECRET} placeholder");
        assertFalse(jwtLine.contains(":"),
                "jwt.secret line must carry no default; unset JWT_SECRET fails startup with CouldNotResolvePlaceholder");
        assertTrue(content.contains("${SMTP_PORT:587}"),
                "other placeholders with defaults (e.g. SMTP_PORT:587) must remain untouched");
    }

    @Test
    @DisplayName("dev compose does not embed the dev fallback secret")
    void devCompose_doesNotEmbedDevSecret() throws IOException {
        String content = read("docker-compose.yml");
        assertFalse(content.contains(DEV_FALLBACK),
                "docker-compose.yml must not hardcode the dev fallback secret");
        assertFalse(content.matches("(?m)^\\s*JWT_SECRET\\s*:.*"),
                "docker-compose.yml must not define a JWT_SECRET mapping line (a comment mentioning JWT_SECRET is fine)");
    }

    @Test
    @DisplayName("prod compose retains fail-fast on JWT_SECRET (read-only)")
    void prodCompose_retainsFailFast() throws IOException {
        String content = read("docker-compose.prod.yml");
        assertTrue(content.contains("JWT_SECRET: ${JWT_SECRET:?"),
                "docker-compose.prod.yml must retain ${JWT_SECRET:?} fail-fast");
    }

    @Test
    @DisplayName("V6 migration exists with idempotent tokens_revocados indexes only")
    void v6Migration_existsWithIdempotentIndexes() throws IOException {
        Path v6 = repoRoot().resolve(
                "src/main/resources/db/migration/V6__tokens_revocados_indexes.sql");
        assertTrue(Files.exists(v6), "V6 migration file must exist");
        assertTrue(v6.getFileName().toString().endsWith(".sql")
                        && v6.getFileName().toString().startsWith("V6__"),
                "V6 migration filename must match Flyway convention V6__*.sql");
        String content = Files.readString(v6);
        assertFalse(content.isBlank(), "V6 migration must be non-empty");
        assertTrue(content.contains("IF NOT EXISTS"),
                "V6 CREATE INDEX statements must be idempotent via IF NOT EXISTS");
        assertTrue(content.contains("idx_tokens_revocados_email_expira"),
                "V6 must create the composite (email, expira_en) index");
        assertTrue(content.contains("idx_tokens_revocados_expira"),
                "V6 must create the single-column (expira_en) index");
        assertTrue(content.contains("tokens_revocados"),
                "V6 must target the tokens_revocados table");
        assertFalse(content.matches("(?s).*(?i)\\b(ALTER\\s+TABLE|DROP\\s+TABLE|DROP\\s+COLUMN)\\b.*"),
                "V6 must be non-destructive: no DROP / ALTER COLUMN");
    }

    @Test
    @DisplayName("V6 migration touches no table other than tokens_revocados")
    void v6Migration_touchesOnlyTokensRevocados() throws IOException {
        String content = read(
                "src/main/resources/db/migration/V6__tokens_revocados_indexes.sql");
        String lower = content.toLowerCase();
        assertFalse(lower.contains("usuario"),
                "V6 must not reference the usuario table");
        assertFalse(lower.contains("libro"),
                "V6 must not reference the libro table");
        assertFalse(lower.contains("prestamo"),
                "V6 must not reference the prestamo table");
        assertFalse(lower.contains("multa"),
                "V6 must not reference the multa table");
    }

    @Test
    @DisplayName("rotation runbook exists, is non-empty and documents procedure")
    void runbook_existsAndDocumentsProcedure() throws IOException {
        Path runbook = repoRoot().resolve("docs/ops/jwt-secret-rotation.md");
        assertTrue(Files.exists(runbook),
                "runbook must exist at docs/ops/jwt-secret-rotation.md");
        String content = Files.readString(runbook);
        assertFalse(content.isBlank(), "runbook must be non-empty");
        assertTrue(content.contains("Last verified:"),
                "runbook must carry a Last verified staleness header");
        assertTrue(content.contains("openssl rand -base64 48"),
                "runbook must document secret generation");
        assertTrue(content.contains("DROP INDEX IF EXISTS"),
                "runbook must document V6 index rollback");
    }

    @Test
    @DisplayName("JwtTokenProvider Javadoc clarifies the dev-only concession")
    void providerJavadoc_documentsDevOnlyConcession() throws IOException {
        String content = read(
                "src/main/java/com/libromagico/security/JwtTokenProvider.java");
        assertTrue(content.contains("8cf9df2"),
                "Javadoc must reference the publishing commit 8cf9df2");
        assertTrue(content.contains("requireProductionSecret"),
                "Javadoc must reference the three-layer guard");
        assertTrue(content.toLowerCase().contains("dev-only")
                        || content.toLowerCase().contains("solo para desarrollo")
                        || content.toLowerCase().contains("desarrollo"),
                "Javadoc must state the fallback is dev-only");
    }

    @Test
    @DisplayName("base properties comment documents the three-layer prod guard")
    void baseProperties_commentDocumentsThreeLayerGuard() throws IOException {
        String content = read("src/main/resources/application.properties");
        assertTrue(content.contains("docker-compose.prod.yml"),
                "comment must reference the compose fail-fast layer");
        assertTrue(content.contains("application-prod.properties"),
                "comment must reference the properties fail-fast layer");
        assertTrue(content.contains("requireProductionSecret"),
                "comment must reference the Java guard layer");
        assertTrue(content.contains(
                "jwt.secret=${JWT_SECRET:" + DEV_FALLBACK + "}"),
                "dev fallback property value line must remain unchanged");
    }
}
