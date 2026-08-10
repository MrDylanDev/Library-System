package com.libromagico.security;

import com.libromagico.model.EstadoUsuario;
import com.libromagico.model.RolUsuario;
import com.libromagico.model.Usuario;
import com.libromagico.repository.PrestamoRepository;
import com.libromagico.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests de integración del rate limiting en los endpoints de autenticación.
 *
 * <p>Esta clase ACTIVA el rate limiting con límites chicos (IP: 3 por minuto,
 * email: 2 cada 15 minutos) para poder disparar el {@code 429}. La IP de
 * MockMvc es siempre {@code 127.0.0.1} y {@code X-Forwarded-For} no se confía
 * por defecto, por eso {@code @BeforeEach} resetea el estado del limitador vía
 * {@link AuthRateLimiter#clear()}.</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:testtx-ratelimit;DB_CLOSE_DELAY=-1",
    "auth.rate-limit.enabled=true",
    "auth.rate-limit.ip.max-attempts=3",
    "auth.rate-limit.ip.window-minutes=1",
    "auth.rate-limit.email.max-attempts=2",
    "auth.rate-limit.email.window-minutes=15",
    "auth.rate-limit.trusted-proxies="
})
class AuthRateLimitIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private PrestamoRepository prestamoRepository;
    @Autowired private AuthRateLimiter authRateLimiter;

    @BeforeEach
    void setUp() {
        prestamoRepository.deleteAll();
        usuarioRepository.deleteAll();
        authRateLimiter.clear();
    }

    private String uniqueEmail(String prefix) {
        return prefix + "." + UUID.randomUUID().toString().substring(0, 8) + "@test.com";
    }

    private Usuario createUser(String email, String dni) {
        var user = new Usuario();
        user.setNombre("Test User");
        user.setEmail(email);
        user.setContrasena(passwordEncoder.encode("Test123!"));
        user.setDni(dni);
        user.setTelefono("+5491122334455");
        user.setRol(RolUsuario.USER);
        user.setEstado(EstadoUsuario.ACTIVO);
        return usuarioRepository.save(user);
    }

    private String loginBody(String email, String password) {
        return """
            {"email":"%s","contrasena":"%s"}
            """.formatted(email, password);
    }

    private String forgotBody(String email) {
        return """
            {"email":"%s"}
            """.formatted(email);
    }

    private String registerBody(String email, String dni) {
        return """
            {"nombre":"Test User","email":"%s","contrasena":"Test123!","dni":"%s","telefono":"+5491122334455"}
            """.formatted(email, dni);
    }

    private void assertTooManyRequests(String endpoint, String body) throws Exception {
        mockMvc.perform(post(endpoint).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.status").value(429))
                .andExpect(jsonPath("$.message").value("Demasiados intentos. Intente nuevamente más tarde."));
    }

    @Test
    @DisplayName("POST /api/auth/login - credenciales incorrectas: 2 fallos reales (401) y el siguiente es 429")
    void loginIncorrectoQuedaBloqueadoPorEmail() throws Exception {
        var email = uniqueEmail("login");

        mockMvc.perform(post("/api/auth/login").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody(email, "incorrecta")))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/auth/login").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody(email, "incorrecta")))
                .andExpect(status().isUnauthorized());

        assertTooManyRequests("/api/auth/login", loginBody(email, "incorrecta"));
    }

    @Test
    @DisplayName("POST /api/auth/login - requests sin token CSRF fallan con 403 y NO consumen cuota (nunca 429)")
    void loginFallidoPorCsrfNoConsumeCuota() throws Exception {
        var email = uniqueEmail("csrf");

        for (int i = 0; i < 6; i++) {
            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(loginBody(email, "incorrecta")))
                    .andExpect(status().isForbidden());
        }
    }

    @Test
    @DisplayName("POST /api/auth/forgot-password - superar el límite de IP devuelve 429")
    void forgotPasswordQuedaBloqueadoPorIp() throws Exception {
        for (int i = 0; i < 3; i++) {
            mockMvc.perform(post("/api/auth/forgot-password").with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(forgotBody(uniqueEmail("forgot"))))
                    .andExpect(status().isOk());
        }

        assertTooManyRequests("/api/auth/forgot-password", forgotBody(uniqueEmail("forgot")));
    }

    @Test
    @DisplayName("POST /api/auth/register - superar el límite de IP devuelve 429")
    void registerQuedaBloqueadoPorIp() throws Exception {
        for (int i = 0; i < 3; i++) {
            mockMvc.perform(post("/api/auth/register").with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(registerBody(uniqueEmail("reg"), "1000000" + i)))
                    .andExpect(status().isCreated());
        }

        assertTooManyRequests("/api/auth/register", registerBody(uniqueEmail("reg"), "10000003"));
    }

    @Test
    @DisplayName("POST /api/auth/login - X-Forwarded-For NO altera la clave de IP por defecto (siempre remoteAddr)")
    void xForwardedForNoAlteraLaClaveIp() throws Exception {
        for (String forwardedIp : List.of("10.0.0.1", "10.0.0.2", "10.0.0.3")) {
            mockMvc.perform(post("/api/auth/forgot-password").with(csrf())
                            .header("X-Forwarded-For", forwardedIp)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(forgotBody(uniqueEmail("xff"))))
                    .andExpect(status().isOk());
        }

        assertTooManyRequests("/api/auth/forgot-password", forgotBody(uniqueEmail("xff")));
    }

    @Test
    @DisplayName("POST /api/auth/login - el usuario legítimo con logins exitosos no queda bloqueado")
    void loginExitosoNoBloqueaAlUsuario() throws Exception {
        var email = uniqueEmail("legit");
        createUser(email, "98765432");

        mockMvc.perform(post("/api/auth/login").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody(email, "Test123!")))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/auth/login").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody(email, "Test123!")))
                .andExpect(status().isOk());
    }
}