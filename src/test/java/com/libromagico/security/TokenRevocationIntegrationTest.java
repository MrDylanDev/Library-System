package com.libromagico.security;

import com.libromagico.TestAuthSupport;
import com.libromagico.model.EstadoUsuario;
import com.libromagico.model.RolUsuario;
import com.libromagico.model.Usuario;
import com.libromagico.repository.TokenRevocadoRepository;
import com.libromagico.repository.UsuarioRepository;
import com.libromagico.service.UsuarioService;
import jakarta.servlet.http.Cookie;
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

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:testtx-revocacion;DB_CLOSE_DELAY=-1"
})
class TokenRevocationIntegrationTest {

    private static final String ADMIN_EMAIL = "admin@libromagico.com";
    private static final String ADMIN_PASS = "admin123";
    private static final String USER_EMAIL = "usuario@libromagico.com";
    private static final String USER_PASS = "usuario123";

    @Autowired private MockMvc mockMvc;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private TokenRevocadoRepository tokenRevocadoRepository;
    @Autowired private UsuarioService usuarioService;

    @BeforeEach
    void setUp() {
        tokenRevocadoRepository.deleteAll();
        usuarioRepository.deleteAll();
        seedUsuario(ADMIN_EMAIL, ADMIN_PASS, RolUsuario.ADMIN, "00000001");
        seedUsuario(USER_EMAIL, USER_PASS, RolUsuario.USER, "00000003");
    }

    private void seedUsuario(String email, String contrasena, RolUsuario rol, String dni) {
        var usuario = new Usuario();
        usuario.setNombre("Seed " + rol.name());
        usuario.setEmail(email);
        usuario.setContrasena(passwordEncoder.encode(contrasena));
        usuario.setDni(dni);
        usuario.setTelefono("+5491122334455");
        usuario.setRol(rol);
        usuario.setEstado(EstadoUsuario.ACTIVO);
        usuarioRepository.save(usuario);
    }

    private String login(String email, String contrasena) throws Exception {
        var response = mockMvc.perform(post("/api/auth/login").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"email":"%s","contrasena":"%s"}
                            """.formatted(email, contrasena)))
                .andExpect(status().isOk())
                .andReturn();
        return TestAuthSupport.extractToken(response);
    }

    private void logout(String token) throws Exception {
        mockMvc.perform(post("/api/auth/logout").with(csrf())
                        .cookie(new Cookie(TestAuthSupport.AUTH_COOKIE, token)))
                .andExpect(status().isOk());
    }

    private Cookie authCookie(String token) {
        return new Cookie(TestAuthSupport.AUTH_COOKIE, token);
    }

    @Test
    @DisplayName("logout agrega el token a la denylist: /api/auth/me con la cookie vieja -> 401")
    void logoutRevocaTokenEnDenylist() throws Exception {
        var token = login(ADMIN_EMAIL, ADMIN_PASS);
        logout(token);

        mockMvc.perform(get("/api/auth/me").cookie(authCookie(token)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("token no revocado sigue autenticando: /api/auth/me -> 200")
    void tokenNoRevocadoSigueAutenticando() throws Exception {
        var token = login(ADMIN_EMAIL, ADMIN_PASS);

        mockMvc.perform(get("/api/auth/me").cookie(authCookie(token)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("token revocado en ruta protegida: GET /api/libros con cookie vieja -> 401")
    void tokenRevocadoEnRutaProtegida() throws Exception {
        var token = login(ADMIN_EMAIL, ADMIN_PASS);
        logout(token);

        mockMvc.perform(get("/api/libros").cookie(authCookie(token)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("resetPassword invalida las sesiones previas del usuario")
    void resetPasswordRevocaTokens() throws Exception {
        var usuario = usuarioRepository.findByEmail(USER_EMAIL).orElseThrow();
        var hashOriginal = usuario.getContrasena();

        var token = login(USER_EMAIL, USER_PASS);

        usuario.setResetToken("reset-token-revocacion");
        usuario.setResetTokenExpiry(LocalDateTime.now().plusHours(1));
        usuarioRepository.save(usuario);

        usuarioService.resetPassword("reset-token-revocacion", "NuevaPass1!");

        mockMvc.perform(get("/api/auth/me").cookie(authCookie(token)))
                .andExpect(status().isUnauthorized());

        var restaurado = usuarioRepository.findByEmail(USER_EMAIL).orElseThrow();
        restaurado.setContrasena(hashOriginal);
        restaurado.setResetToken(null);
        restaurado.setResetTokenExpiry(null);
        usuarioRepository.save(restaurado);
    }

    @Test
    @DisplayName("segundo reset consecutivo del mismo usuario no falla (regresión E2E)")
    void segundoResetConsecutivoNoFalla() throws Exception {
        // Primera restauración: crea el marcador por email
        var u1 = usuarioRepository.findByEmail(USER_EMAIL).orElseThrow();
        u1.setResetToken("reset-1");
        u1.setResetTokenExpiry(LocalDateTime.now().plusHours(1));
        usuarioRepository.save(u1);
        usuarioService.resetPassword("reset-1", "NuevaPass1!");

        // Segunda restauración del mismo email: debe funcionar sin DataIntegrityViolation
        var u2 = usuarioRepository.findByEmail(USER_EMAIL).orElseThrow();
        u2.setResetToken("reset-2");
        u2.setResetTokenExpiry(LocalDateTime.now().plusHours(1));
        usuarioRepository.save(u2);
        usuarioService.resetPassword("reset-2", "OtraPass2!");

        // Restaurar el password original para no contaminar otros tests
        var restaurado = usuarioRepository.findByEmail(USER_EMAIL).orElseThrow();
        restaurado.setContrasena(passwordEncoder.encode(USER_PASS));
        restaurado.setResetToken(null);
        restaurado.setResetTokenExpiry(null);
        usuarioRepository.save(restaurado);
    }
}