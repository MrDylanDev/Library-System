package com.libromagico.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.libromagico.dto.AuthResponse;
import com.libromagico.model.RolUsuario;
import com.libromagico.model.Usuario;
import com.libromagico.repository.LibroRepository;
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

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:testtx-catalogo;DB_CLOSE_DELAY=-1"
})
class CatalogControllerIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private LibroRepository libroRepository;
    @Autowired private PrestamoRepository prestamoRepository;

    @BeforeEach
    void setUp() {
        prestamoRepository.deleteAll();
        libroRepository.deleteAll();
        usuarioRepository.deleteAll();
    }

    private String uniqueEmail(String prefix) {
        return prefix + "." + UUID.randomUUID().toString().substring(0, 8) + "@test.com";
    }

    private AuthResponse createLibrarianAndLogin() throws Exception {
        String email = uniqueEmail("biblio");
        var librarian = new Usuario();
        librarian.setNombre("Bibliotecario");
        librarian.setEmail(email);
        librarian.setContrasena(passwordEncoder.encode("Biblio123!"));
        librarian.setDni("11111111");
        librarian.setTelefono("+5491111111111");
        librarian.setRol(RolUsuario.LIBRARIAN);
        usuarioRepository.save(librarian);

        var response = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"email":"%s","contrasena":"Biblio123!"}
                            """.formatted(email)))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readValue(response.getResponse().getContentAsString(), AuthResponse.class);
    }

    @Test
    @DisplayName("GET /api/catalogo - listar todos los libros sin autenticación")
    void listarCatalogoSinAuth() throws Exception {
        var auth = createLibrarianAndLogin();
        mockMvc.perform(post("/api/libros")
                        .header("Authorization", "Bearer " + auth.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"isbn":"9780132350884","titulo":"Clean Code","autor":"Robert Martin","categoria":"Software","añoPub":2008,"editorial":"Prentice Hall"}
                            """))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/api/libros")
                        .header("Authorization", "Bearer " + auth.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"isbn":"9780201633610","titulo":"Design Patterns","autor":"Gang of Four","categoria":"Software","añoPub":1994,"editorial":"Addison-Wesley"}
                            """))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/catalogo"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    @DisplayName("GET /api/catalogo/{isbn} - detalle de libro por ISBN")
    void detalleLibroPorIsbn() throws Exception {
        var auth = createLibrarianAndLogin();
        mockMvc.perform(post("/api/libros")
                        .header("Authorization", "Bearer " + auth.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"isbn":"9780132350884","titulo":"Clean Code","autor":"Robert Martin","categoria":"Software","añoPub":2008,"editorial":"Prentice Hall"}
                            """))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/catalogo/9780132350884"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isbn").value("9780132350884"))
                .andExpect(jsonPath("$.titulo").value("Clean Code"))
                .andExpect(jsonPath("$.autor").value("Robert Martin"));
    }

    @Test
    @DisplayName("GET /api/catalogo/buscar?q=keyword - buscar libros por palabra clave")
    void buscarLibrosPorKeyword() throws Exception {
        var auth = createLibrarianAndLogin();
        mockMvc.perform(post("/api/libros")
                        .header("Authorization", "Bearer " + auth.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"isbn":"9780132350884","titulo":"Clean Code","autor":"Robert Martin","categoria":"Software","añoPub":2008,"editorial":"Prentice Hall"}
                            """))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/api/libros")
                        .header("Authorization", "Bearer " + auth.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"isbn":"9780201633610","titulo":"Design Patterns","autor":"Gang of Four","categoria":"Software","añoPub":1994,"editorial":"Addison-Wesley"}
                            """))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/catalogo/buscar?q=clean"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].titulo").value("Clean Code"));
    }

    @Test
    @DisplayName("GET /api/catalogo/9999999999 - libro no encontrado devuelve 404")
    void libroNoEncontrado() throws Exception {
        mockMvc.perform(get("/api/catalogo/9999999999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Libro no encontrado: 9999999999"));
    }
}
