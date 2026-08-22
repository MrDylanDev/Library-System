package com.libromagico.controller;

import com.libromagico.TestAuthSupport;
import com.libromagico.model.*;
import com.libromagico.repository.*;
import com.libromagico.service.UsuarioService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Regresión: la API no debe exponer campos internos (hash de contraseña,
 * token de reset) en las respuestas de préstamos y multas, ni siquiera a
 * través de las relaciones anidadas usuario/libro.
 *
 * @see <a href="https://github.com/MrDylanDev/Library-System/issues/84">#84</a>
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:test-entity-leak;DB_CLOSE_DELAY=-1"
})
class EntityLeakIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private LibroRepository libroRepository;
    @Autowired private PrestamoRepository prestamoRepository;
    @Autowired private MultaRepository multaRepository;

    @BeforeEach
    void setUp() {
        multaRepository.deleteAll();
        prestamoRepository.deleteAll();
        libroRepository.deleteAll();
        usuarioRepository.deleteAll();
    }

    private String uniqueEmail(String prefix) {
        return prefix + "." + UUID.randomUUID().toString().substring(0, 8) + "@test.com";
    }

    private Prestamo createPrestamo(Usuario usuario) {
        var libro = new Libro();
        libro.setIsbn("9780451524934");
        libro.setTitulo("1984");
        libro.setAutor("George Orwell");
        libro.setCopiasDisponibles(1);
        libro.setEstado(EstadoLibro.DISPONIBLE);
        libroRepository.save(libro);

        var prestamo = new Prestamo();
        prestamo.setUsuario(usuario);
        prestamo.setLibro(libro);
        prestamo.setFechaPrestamo(LocalDate.now().minusDays(10));
        prestamo.setFechaDevolucion(LocalDate.now().plusDays(5));
        prestamo.setEstado(EstadoPrestamo.ACTIVO);
        return prestamoRepository.save(prestamo);
    }

    @Test
    @DisplayName("GET /api/prestamos no expone contrasena ni resetToken del usuario")
    void prestamos_noExponenCamposSensibles() throws Exception {
        var user = new Usuario();
        user.setNombre("User Leak");
        user.setEmail(uniqueEmail("leak"));
        user.setContrasena(passwordEncoder.encode("Secret123!"));
        user.setDni("55555555");
        user.setRol(RolUsuario.USER);
        user.setResetTokenHash(UsuarioService.hashToken("supersecret-token"));
        user.setResetTokenExpiry(LocalDate.now().atStartOfDay().plusHours(5));
        usuarioRepository.save(user);
        createPrestamo(user);

        var loginResponse = mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType("application/json")
                        .content("""
                            {"email":"%s","contrasena":"Secret123!"}""".formatted(user.getEmail())))
                .andReturn();
        var token = TestAuthSupport.extractToken(loginResponse);

        mockMvc.perform(get("/api/prestamos/usuarios/" + user.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].usuario.nombre").value("User Leak"))
                .andExpect(jsonPath("$.content[0].libro.titulo").value("1984"))
                .andExpect(content().string(not(containsString("Secret123!"))))
                .andExpect(content().string(not(containsString("supersecret-token"))));
    }

    @Test
    @DisplayName("GET /api/admin/prestamos no expone contrasena del usuario")
    void adminPrestamos_noExponenContrasena() throws Exception {
        var admin = new Usuario();
        admin.setNombre("Admin");
        admin.setEmail(uniqueEmail("admin2"));
        admin.setContrasena(passwordEncoder.encode("Admin123!"));
        admin.setDni("99999998");
        admin.setRol(RolUsuario.ADMIN);
        usuarioRepository.save(admin);

        var user = new Usuario();
        user.setNombre("User Leak 2");
        user.setEmail(uniqueEmail("leak2"));
        user.setContrasena(passwordEncoder.encode("Hidden123!"));
        user.setDni("55555554");
        user.setRol(RolUsuario.USER);
        usuarioRepository.save(user);
        createPrestamo(user);

        var loginResponse = mockMvc.perform(org.springframework.test.web.servlet.request
                        .MockMvcRequestBuilders.post("/api/auth/login")
                        .with(csrf())
                        .contentType("application/json")
                        .content("""
                            {"email":"%s","contrasena":"Admin123!"}""".formatted(admin.getEmail())))
                .andReturn();
        var token = TestAuthSupport.extractToken(loginResponse);

        mockMvc.perform(get("/api/admin/prestamos")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].usuario.nombre").value("User Leak 2"))
                .andExpect(content().string(not(containsString("Hidden123!"))));
    }
}
