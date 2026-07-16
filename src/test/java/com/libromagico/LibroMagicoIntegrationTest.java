package com.libromagico;

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

import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:testtx-libromagico;DB_CLOSE_DELAY=-1"
})
class LibroMagicoIntegrationTest {

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

    private AuthResponse registerUser(String email) throws Exception {
        var response = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"nombre":"Dylan","email":"%s","contrasena":"Test123!","dni":"12345678","telefono":"+5491122334455"}
                            """.formatted(email)))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readValue(response.getResponse().getContentAsString(), AuthResponse.class);
    }

    @Test
    @DisplayName("Registrar usuario normal y obtener token")
    void registrarUsuario() throws Exception {
        var email = uniqueEmail("dylan");
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"nombre":"Dylan","email":"%s","contrasena":"Test123!","dni":"12345678","telefono":"+5491122334455"}
                            """.formatted(email)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").exists())
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.rol").value("USER"));
    }

    @Test
    @DisplayName("No permitir email duplicado")
    void emailDuplicado() throws Exception {
        var email = uniqueEmail("dylan");

        // First registration succeeds
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"nombre":"Dylan","email":"%s","contrasena":"Test123!","dni":"12345678","telefono":"+5491122334455"}
                            """.formatted(email)))
                .andExpect(status().isCreated());

        // Duplicate email is rejected
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"nombre":"Otro","email":"%s","contrasena":"Test456!","dni":"87654321","telefono":"+5491122334466"}
                            """.formatted(email)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("El email ya está registrado"));
    }

    @Test
    @DisplayName("Iniciar sesión como bibliotecario")
    void loginLibrarian() throws Exception {
        var email = uniqueEmail("biblio");
        var librarian = new Usuario();
        librarian.setNombre("Bibliotecario");
        librarian.setEmail(email);
        librarian.setContrasena(passwordEncoder.encode("Biblio123!"));
        librarian.setDni("11111111");
        librarian.setTelefono("+5491111111111");
        librarian.setRol(RolUsuario.LIBRARIAN);
        usuarioRepository.save(librarian);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"email":"%s","contrasena":"Biblio123!"}
                            """.formatted(email)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rol").value("LIBRARIAN"))
                .andExpect(jsonPath("$.token").exists());
    }

    @Test
    @DisplayName("Crear un libro - bibliotecario")
    void crearLibro() throws Exception {
        var auth = createLibrarianAndLogin();
        var isbn = "9780132350884";

        mockMvc.perform(post("/api/libros")
                        .header("Authorization", "Bearer " + auth.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"isbn":"%s","titulo":"Clean Code","autor":"Robert Martin","categoria":"Software","añoPub":2008,"editorial":"Prentice Hall"}
                            """.formatted(isbn)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.isbn").value(isbn))
                .andExpect(jsonPath("$.titulo").value("Clean Code"))
                .andExpect(jsonPath("$.estado").value("DISPONIBLE"));
    }

    @Test
    @DisplayName("Listar todos los libros")
    void listarLibros() throws Exception {
        var librarian = createLibrarianAndLogin();
        var isbn = "9780132350884";

        mockMvc.perform(post("/api/libros")
                        .header("Authorization", "Bearer " + librarian.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"isbn":"%s","titulo":"Clean Code","autor":"Robert Martin","categoria":"Software","añoPub":2008,"editorial":"Prentice Hall"}
                            """.formatted(isbn)))
                .andExpect(status().isCreated());

        var user = registerUser(uniqueEmail("user"));

        mockMvc.perform(get("/api/libros")
                        .header("Authorization", "Bearer " + user.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].titulo").value("Clean Code"));
    }

    @Test
    @DisplayName("Buscar libro por ISBN")
    void buscarPorIsbn() throws Exception {
        var librarian = createLibrarianAndLogin();
        var isbn = "9780132350884";

        mockMvc.perform(post("/api/libros")
                        .header("Authorization", "Bearer " + librarian.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"isbn":"%s","titulo":"Clean Code","autor":"Robert Martin","categoria":"Software","añoPub":2008,"editorial":"Prentice Hall"}
                            """.formatted(isbn)))
                .andExpect(status().isCreated());

        var user = registerUser(uniqueEmail("user"));

        mockMvc.perform(get("/api/libros/" + isbn)
                        .header("Authorization", "Bearer " + user.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.autor").value("Robert Martin"));
    }

    @Test
    @DisplayName("Buscar libro por autor")
    void buscarPorAutor() throws Exception {
        var librarian = createLibrarianAndLogin();
        var isbn = "9780132350884";

        mockMvc.perform(post("/api/libros")
                        .header("Authorization", "Bearer " + librarian.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"isbn":"%s","titulo":"Clean Code","autor":"Robert Martin","categoria":"Software","añoPub":2008,"editorial":"Prentice Hall"}
                            """.formatted(isbn)))
                .andExpect(status().isCreated());

        var user = registerUser(uniqueEmail("user"));

        mockMvc.perform(get("/api/libros/buscar?autor=robert")
                        .header("Authorization", "Bearer " + user.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    @DisplayName("Actualizar libro")
    void actualizarLibro() throws Exception {
        var auth = createLibrarianAndLogin();
        var isbn = "9780132350884";

        mockMvc.perform(post("/api/libros")
                        .header("Authorization", "Bearer " + auth.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"isbn":"%s","titulo":"Clean Code","autor":"Robert Martin","categoria":"Software","añoPub":2008,"editorial":"Prentice Hall"}
                            """.formatted(isbn)))
                .andExpect(status().isCreated());

        mockMvc.perform(put("/api/libros/" + isbn)
                        .header("Authorization", "Bearer " + auth.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"isbn":"%s","titulo":"Clean Code 2nd Ed","autor":"Robert C. Martin","categoria":"Software","añoPub":2008,"editorial":"Prentice Hall"}
                            """.formatted(isbn)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.titulo").value("Clean Code 2nd Ed"));
    }

    @Test
    @DisplayName("Usuario sin permisos no puede eliminar libro")
    void eliminarLibroSinPermiso() throws Exception {
        var librarian = createLibrarianAndLogin();
        var isbn = "9780132350884";

        mockMvc.perform(post("/api/libros")
                        .header("Authorization", "Bearer " + librarian.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"isbn":"%s","titulo":"Clean Code","autor":"Robert Martin","categoria":"Software","añoPub":2008,"editorial":"Prentice Hall"}
                            """.formatted(isbn)))
                .andExpect(status().isCreated());

        var user = registerUser(uniqueEmail("user"));

        mockMvc.perform(delete("/api/libros/" + isbn)
                        .header("Authorization", "Bearer " + user.token()))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Libro no encontrado devuelve 404")
    void libroNoEncontrado() throws Exception {
        var user = registerUser(uniqueEmail("user"));

        mockMvc.perform(get("/api/libros/9999999999")
                        .header("Authorization", "Bearer " + user.token()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Libro no encontrado: 9999999999"));
    }

    @Test
    @DisplayName("Crear segundo libro para préstamos")
    void crearSegundoLibro() throws Exception {
        var auth = createLibrarianAndLogin();
        var isbn = "9780201633610";

        mockMvc.perform(post("/api/libros")
                        .header("Authorization", "Bearer " + auth.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"isbn":"%s","titulo":"Design Patterns","autor":"Gang of Four","categoria":"Software","añoPub":1994,"editorial":"Addison-Wesley"}
                            """.formatted(isbn)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.isbn").value(isbn))
                .andExpect(jsonPath("$.titulo").value("Design Patterns"));
    }

    @Test
    @DisplayName("Realizar préstamo de libro")
    void realizarPrestamo() throws Exception {
        var librarian = createLibrarianAndLogin();
        var isbn = "9780132350884";

        mockMvc.perform(post("/api/libros")
                        .header("Authorization", "Bearer " + librarian.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"isbn":"%s","titulo":"Clean Code","autor":"Robert Martin","categoria":"Software","añoPub":2008,"editorial":"Prentice Hall"}
                            """.formatted(isbn)))
                .andExpect(status().isCreated());

        var user = registerUser(uniqueEmail("user"));

        mockMvc.perform(post("/api/prestamos")
                        .header("Authorization", "Bearer " + user.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"usuarioId":%d,"libroIsbn":"%s"}
                            """.formatted(user.id(), isbn)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.estado").value("ACTIVO"));
    }

    @Test
    @DisplayName("No permitir préstamo duplicado del mismo libro")
    void prestamoDuplicado() throws Exception {
        var librarian = createLibrarianAndLogin();
        var isbn = "9780132350884";

        mockMvc.perform(post("/api/libros")
                        .header("Authorization", "Bearer " + librarian.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"isbn":"%s","titulo":"Clean Code","autor":"Robert Martin","categoria":"Software","añoPub":2008,"editorial":"Prentice Hall"}
                            """.formatted(isbn)))
                .andExpect(status().isCreated());

        var user = registerUser(uniqueEmail("user"));

        // First loan succeeds
        mockMvc.perform(post("/api/prestamos")
                        .header("Authorization", "Bearer " + user.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"usuarioId":%d,"libroIsbn":"%s"}
                            """.formatted(user.id(), isbn)))
                .andExpect(status().isCreated());

        // Duplicate loan is rejected
        mockMvc.perform(post("/api/prestamos")
                        .header("Authorization", "Bearer " + user.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"usuarioId":%d,"libroIsbn":"%s"}
                            """.formatted(user.id(), isbn)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("El usuario ya tiene un préstamo activo de este libro"));
    }

    @Test
    @DisplayName("Consultar historial de préstamos del usuario")
    void historialPrestamos() throws Exception {
        var librarian = createLibrarianAndLogin();
        var isbn = "9780132350884";

        mockMvc.perform(post("/api/libros")
                        .header("Authorization", "Bearer " + librarian.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"isbn":"%s","titulo":"Clean Code","autor":"Robert Martin","categoria":"Software","añoPub":2008,"editorial":"Prentice Hall"}
                            """.formatted(isbn)))
                .andExpect(status().isCreated());

        var user = registerUser(uniqueEmail("user"));

        mockMvc.perform(post("/api/prestamos")
                        .header("Authorization", "Bearer " + user.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"usuarioId":%d,"libroIsbn":"%s"}
                            """.formatted(user.id(), isbn)))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/prestamos/usuarios/" + user.id())
                        .header("Authorization", "Bearer " + user.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].libro.titulo").value("Clean Code"));
    }

    @Test
    @DisplayName("Devolver libro")
    void devolverLibro() throws Exception {
        var librarian = createLibrarianAndLogin();
        var isbn = "9780132350884";

        mockMvc.perform(post("/api/libros")
                        .header("Authorization", "Bearer " + librarian.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"isbn":"%s","titulo":"Clean Code","autor":"Robert Martin","categoria":"Software","añoPub":2008,"editorial":"Prentice Hall"}
                            """.formatted(isbn)))
                .andExpect(status().isCreated());

        var user = registerUser(uniqueEmail("user"));

        var prestamoResponse = mockMvc.perform(post("/api/prestamos")
                        .header("Authorization", "Bearer " + user.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"usuarioId":%d,"libroIsbn":"%s"}
                            """.formatted(user.id(), isbn)))
                .andExpect(status().isCreated())
                .andReturn();

        var prestamoBody = objectMapper.readValue(prestamoResponse.getResponse().getContentAsString(), Map.class);
        var prestamoId = ((Number) prestamoBody.get("id")).longValue();

        mockMvc.perform(put("/api/prestamos/" + prestamoId + "/devolucion")
                        .header("Authorization", "Bearer " + librarian.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("DEVUELTO"))
                .andExpect(jsonPath("$.fechaEntregaReal").exists());
    }

    @Test
    @DisplayName("No permitir devolver préstamo ya devuelto")
    void devolverYaDevuelto() throws Exception {
        var librarian = createLibrarianAndLogin();
        var isbn = "9780132350884";

        mockMvc.perform(post("/api/libros")
                        .header("Authorization", "Bearer " + librarian.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"isbn":"%s","titulo":"Clean Code","autor":"Robert Martin","categoria":"Software","añoPub":2008,"editorial":"Prentice Hall"}
                            """.formatted(isbn)))
                .andExpect(status().isCreated());

        var user = registerUser(uniqueEmail("user"));

        var prestamoResponse = mockMvc.perform(post("/api/prestamos")
                        .header("Authorization", "Bearer " + user.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"usuarioId":%d,"libroIsbn":"%s"}
                            """.formatted(user.id(), isbn)))
                .andExpect(status().isCreated())
                .andReturn();

        var prestamoBody = objectMapper.readValue(prestamoResponse.getResponse().getContentAsString(), Map.class);
        var prestamoId = ((Number) prestamoBody.get("id")).longValue();

        // Return the book first
        mockMvc.perform(put("/api/prestamos/" + prestamoId + "/devolucion")
                        .header("Authorization", "Bearer " + librarian.token()))
                .andExpect(status().isOk());

        // Second return is rejected
        mockMvc.perform(put("/api/prestamos/" + prestamoId + "/devolucion")
                        .header("Authorization", "Bearer " + librarian.token()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("El préstamo ya fue devuelto"));
    }

    @Test
    @DisplayName("GET /api/usuarios/{id} omite contrasena, resetToken, resetTokenExpiry")
    void obtenerUsuarioNoExponeCamposSensibles() throws Exception {
        var email = uniqueEmail("biblio");
        var librarian = new Usuario();
        librarian.setNombre("Bibliotecario");
        librarian.setEmail(email);
        librarian.setContrasena(passwordEncoder.encode("Biblio123!"));
        librarian.setDni("11111111");
        librarian.setTelefono("+5491111111111");
        librarian.setRol(RolUsuario.LIBRARIAN);
        var saved = usuarioRepository.save(librarian);
        var librarianId = saved.getId();

        var auth = objectMapper.readValue(
                mockMvc.perform(post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                    {"email":"%s","contrasena":"Biblio123!"}
                                    """.formatted(email)))
                        .andExpect(status().isOk())
                        .andReturn()
                        .getResponse()
                        .getContentAsString(),
                AuthResponse.class);

        mockMvc.perform(get("/api/usuarios/" + librarianId)
                        .header("Authorization", "Bearer " + auth.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(librarianId))
                .andExpect(jsonPath("$.nombre").value("Bibliotecario"))
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.dni").value("11111111"))
                .andExpect(jsonPath("$.telefono").value("+5491111111111"))
                .andExpect(jsonPath("$.rol").value("LIBRARIAN"))
                .andExpect(jsonPath("$.estado").value("ACTIVO"))
                .andExpect(jsonPath("$.contrasena").doesNotExist())
                .andExpect(jsonPath("$.resetToken").doesNotExist())
                .andExpect(jsonPath("$.resetTokenExpiry").doesNotExist());
    }

    @Test
    @DisplayName("JWT inválido devuelve 401 con formato JSON GlobalExceptionHandler")
    void jwtInvalidoDevuelve401ConFormatoCorrecto() throws Exception {
        mockMvc.perform(get("/api/libros")
                        .header("Authorization", "Bearer token-invalido"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.message").exists());
    }
}
