package com.libromagico.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Verifica los headers de seguridad de la API: Content-Security-Policy presente
 * en las respuestas y la consola H2 NO expuesta fuera del perfil dev.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityHeadersTest {

    @Autowired private MockMvc mockMvc;

    @Test
    @DisplayName("Las respuestas incluyen Content-Security-Policy estricta sin unsafe-inline en scripts")
    void cspHeaderPresente() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andReturn();

        String csp = result.getResponse().getHeader("Content-Security-Policy");
        assertThat(csp)
                .isNotNull()
                .contains("default-src 'self'")
                .contains("script-src 'self'")
                // No debe permitir scripts inline en la app principal: la barrera
                // contra XSS vía innerHTML depende de que NO exista unsafe-inline.
                .doesNotContain("script-src 'self' 'unsafe-inline'")
                .contains("object-src 'none'")
                .contains("base-uri 'self'");
    }

    @Test
    @DisplayName("La consola H2 devuelve 401 fuera del perfil dev (no queda pública)")
    void h2ConsoleBloqueadaFueraDeDev() throws Exception {
        mockMvc.perform(get("/h2-console/"))
                .andExpect(status().isUnauthorized());
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "/api/health",
        "/api/catalogo",
        "/",
    })
    @DisplayName("CSP presente en rutas principales")
    void cspEnRutas(String path) throws Exception {
        mockMvc.perform(get(path))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Security-Policy",
                    org.hamcrest.Matchers.containsString("default-src 'self'")));
    }

    @Test
    @DisplayName("Actuator /health es público (para healthcheck) y reporta UP")
    void actuatorHealthPublico() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Actuator /metrics requiere autenticación (401 anónimo)")
    void actuatorMetricsProtegido() throws Exception {
        mockMvc.perform(get("/actuator/metrics"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Actuator /info requiere autenticación (401 anónimo)")
    void actuatorInfoProtegido() throws Exception {
        mockMvc.perform(get("/actuator/info"))
                .andExpect(status().isUnauthorized());
    }
}