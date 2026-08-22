package com.libromagico.security;

import com.libromagico.TestAuthSupport;
import com.libromagico.model.EstadoUsuario;
import com.libromagico.model.RolUsuario;
import com.libromagico.model.Usuario;
import com.libromagico.repository.PrestamoRepository;
import com.libromagico.repository.UsuarioRepository;
import com.libromagico.service.UsuarioService;
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

import java.time.LocalDateTime;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:testtx-authflow;DB_CLOSE_DELAY=-1"
})
class AuthControllerIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private PrestamoRepository prestamoRepository;

    @BeforeEach
    void setUp() {
        prestamoRepository.deleteAll();
        usuarioRepository.deleteAll();
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

    private String loginAndGetToken(String email) throws Exception {
        var response = mockMvc.perform(post("/api/auth/login").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"email":"%s","contrasena":"Test123!"}
                            """.formatted(email)))
                .andExpect(status().isOk())
                .andReturn();
        return TestAuthSupport.extractToken(response);
    }

    @Test
    @DisplayName("GET /api/auth/me - devuelve perfil del usuario autenticado")
    void meAutenticado() throws Exception {
        var email = uniqueEmail("meuser");
        createUser(email, "12345678");
        var token = loginAndGetToken(email);

        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.nombre").value("Test User"))
                .andExpect(jsonPath("$.rol").value("USER"));
    }

    @Test
    @DisplayName("GET /api/auth/me - sin token devuelve 401")
    void meSinAuth() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /api/auth/forgot-password - email existente devuelve éxito")
    void forgotPasswordEmailExistente() throws Exception {
        var email = uniqueEmail("forgot");
        createUser(email, "23456789");

        mockMvc.perform(post("/api/auth/forgot-password").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"email":"%s"}
                            """.formatted(email)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Si el email existe, recibirás un enlace de recuperación"));
    }

    @Test
    @DisplayName("POST /api/auth/forgot-password - email inexistente también devuelve éxito (seguridad)")
    void forgotPasswordEmailInexistente() throws Exception {
        mockMvc.perform(post("/api/auth/forgot-password").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"email":"noexiste@test.com"}
                            """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Si el email existe, recibirás un enlace de recuperación"));
    }

    @Test
    @DisplayName("POST /api/auth/reset-password - token válido cambia la contraseña")
    void resetPasswordTokenValido() throws Exception {
        var email = uniqueEmail("resetok");
        var user = createUser(email, "34567890");
        user.setResetTokenHash(UsuarioService.hashToken("token-valido-123"));
        user.setResetTokenExpiry(LocalDateTime.now().plusHours(1));
        usuarioRepository.save(user);

        mockMvc.perform(post("/api/auth/reset-password").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"token":"token-valido-123","newPassword":"NuevaPass1!"}
                            """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Contraseña actualizada correctamente"));
    }

    @Test
    @DisplayName("POST /api/auth/reset-password - token inválido devuelve 409")
    void resetPasswordTokenInvalido() throws Exception {
        mockMvc.perform(post("/api/auth/reset-password").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"token":"token-invalido","newPassword":"NuevaPass1!"}
                            """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Token inválido o expirado"));
    }

    @Test
    @DisplayName("POST /api/auth/reset-password - token expirado devuelve 409")
    void resetPasswordTokenExpirado() throws Exception {
        var email = uniqueEmail("resetexp");
        var user = createUser(email, "45678901");
        user.setResetTokenHash(UsuarioService.hashToken("token-expirado"));
        user.setResetTokenExpiry(LocalDateTime.now().minusHours(1));
        usuarioRepository.save(user);

        mockMvc.perform(post("/api/auth/reset-password").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"token":"token-expirado","newPassword":"NuevaPass1!"}
                            """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Token inválido o expirado"));
    }
}
