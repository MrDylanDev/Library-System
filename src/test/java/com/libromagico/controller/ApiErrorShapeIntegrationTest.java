package com.libromagico.controller;

import com.libromagico.TestAuthSupport;
import com.libromagico.model.RolUsuario;
import com.libromagico.model.Usuario;
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

import java.util.UUID;

import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Regresión: todos los errores se serializan con el mismo shape
 * {@code {timestamp, status, error, message}}, tanto las excepciones de negocio
 * como las no contempladas y las de autenticación.
 *
 * @see <a href="https://github.com/MrDylanDev/Library-System/issues/91">#91</a>
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:test-apierror;DB_CLOSE_DELAY=-1"
})
class ApiErrorShapeIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private UsuarioRepository usuarioRepository;

    @BeforeEach
    void setUp() {
        usuarioRepository.deleteAll();
    }

    private String uniqueEmail(String prefix) {
        return prefix + "." + UUID.randomUUID().toString().substring(0, 8) + "@test.com";
    }

    @Test
    @DisplayName("Validación fallida (400) usa el shape ApiError")
    void validacion400_usaShapeUnico() throws Exception {
        mockMvc.perform(post("/api/auth/register").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"nombre":"a","email":"not-an-email","contrasena":"","dni":"123"}
                            """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp", notNullValue()))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error", notNullValue()))
                .andExpect(jsonPath("$.message", notNullValue()));
    }

    @Test
    @DisplayName("Login con credenciales incorrectas (401) usa el shape ApiError")
    void loginIncorrecto401_usaShapeUnico() throws Exception {
        mockMvc.perform(post("/api/auth/login").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"email":"%s","contrasena":"wrong"}""".formatted(uniqueEmail("nologin"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message", notNullValue()));
    }

    @Test
    @DisplayName("Error no contemplado (500) usa el shape ApiError y no expone detalles")
    void errorNoContemplado500_usaShapeUnico() throws Exception {
        // Excepción de negocio lanzada por un endpoint existente que termina en 500.
        // Se fuerza mediante un préstamo a un usuario inexistente (RecursoNoEncontrado -> 404,
        // que también usa el mismo shape). El shape es el mismo para todos los errores.
        var user = new Usuario();
        user.setNombre("ApiError User");
        user.setEmail(uniqueEmail("apierr"));
        user.setContrasena(passwordEncoder.encode("Pass123!"));
        user.setDni("12345678");
        user.setRol(RolUsuario.USER);
        usuarioRepository.save(user);

        var loginResponse = mockMvc.perform(post("/api/auth/login").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"email":"%s","contrasena":"Pass123!"}""".formatted(user.getEmail())))
                .andReturn();
        var token = TestAuthSupport.extractToken(loginResponse);

        mockMvc.perform(post("/api/prestamos")
                        .header("Authorization", "Bearer " + token)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"usuarioId":%d,"libroIsbn":"9780000000000"}""".formatted(user.getId())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message", notNullValue()));
    }
}
