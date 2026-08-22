package com.libromagico.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.env.Environment;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class JwtTokenProviderSecretValidationTest {

    private static final String DEV_FALLBACK =
            "LibroMagico2024SecretKeyParaFirmarJWTMinimo256Bits!!";

    private Environment prodEnv() {
        var env = mock(Environment.class);
        when(env.getActiveProfiles()).thenReturn(new String[]{"prod"});
        return env;
    }

    private Environment devEnv() {
        var env = mock(Environment.class);
        when(env.getActiveProfiles()).thenReturn(new String[]{"dev"});
        return env;
    }

    @Test
    @DisplayName("prod con JWT_SECRET válida no falla")
    void prod_conSecretValida_noFalla() {
        assertDoesNotThrow(() -> new JwtTokenProvider(
                "A".repeat(48), 86400000L, prodEnv()));
    }

    @Test
    @DisplayName("prod con secret por defecto de desarrollo falla")
    void prod_conSecretDefault_falla() {
        assertThrows(IllegalStateException.class, () -> new JwtTokenProvider(
                DEV_FALLBACK, 86400000L, prodEnv()));
    }

    @Test
    @DisplayName("prod con secret demasiado corta falla")
    void prod_conSecretCorta_falla() {
        assertThrows(IllegalStateException.class, () -> new JwtTokenProvider(
                "only-24-bytes-secret!!!", 86400000L, prodEnv()));
    }

    @Test
    @DisplayName("dev con secret default no falla (fallback de desarrollo permitido)")
    void dev_conSecretDefault_noFalla() {
        assertDoesNotThrow(() -> new JwtTokenProvider(
                DEV_FALLBACK, 86400000L, devEnv()));
    }
}
