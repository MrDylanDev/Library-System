package com.libromagico;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.libromagico.model.RolUsuario;
import com.libromagico.model.Usuario;
import com.libromagico.repository.UsuarioRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ActiveProfiles("test")
class LibroMagicoIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private UsuarioRepository usuarioRepository;

    private static String userToken;
    private static String librarianToken;

    @BeforeAll
    void seedLibrarian() {
        var librarian = new Usuario();
        librarian.setNombre("Bibliotecario");
        librarian.setEmail("biblio@test.com");
        librarian.setContrasena(passwordEncoder.encode("Biblio123!"));
        librarian.setDni("11111111");
        librarian.setTelefono("+5491111111111");
        librarian.setRol(RolUsuario.LIBRARIAN);
        usuarioRepository.save(librarian);
    }

    @Test
    @Order(1)
    @DisplayName("Registrar usuario normal y obtener token")
    void registrarUsuario() throws Exception {
        var response = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"nombre":"Dylan","email":"dylan@test.com","contrasena":"Test123!","dni":"12345678","telefono":"+5491122334455"}
                            """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").exists())
                .andExpect(jsonPath("$.email").value("dylan@test.com"))
                .andExpect(jsonPath("$.rol").value("USER"))
                .andReturn();

        var body = objectMapper.readValue(response.getResponse().getContentAsString(), Map.class);
        userToken = (String) body.get("token");
    }

    @Test
    @Order(2)
    @DisplayName("No permitir email duplicado")
    void emailDuplicado() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"nombre":"Otro","email":"dylan@test.com","contrasena":"Test456!","dni":"87654321","telefono":"+5491122334466"}
                            """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("El email ya está registrado"));
    }

    @Test
    @Order(3)
    @DisplayName("Iniciar sesión como bibliotecario")
    void loginLibrarian() throws Exception {
        var response = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"email":"biblio@test.com","contrasena":"Biblio123!"}
                            """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rol").value("LIBRARIAN"))
                .andReturn();

        var body = objectMapper.readValue(response.getResponse().getContentAsString(), Map.class);
        librarianToken = (String) body.get("token");
    }

    @Test
    @Order(4)
    @DisplayName("Crear un libro - bibliotecario")
    void crearLibro() throws Exception {
        mockMvc.perform(post("/api/libros")
                        .header("Authorization", "Bearer " + librarianToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"isbn":"9780132350884","titulo":"Clean Code","autor":"Robert Martin","categoria":"Software","añoPub":2008,"editorial":"Prentice Hall"}
                            """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.isbn").value("9780132350884"))
                .andExpect(jsonPath("$.titulo").value("Clean Code"))
                .andExpect(jsonPath("$.estado").value("DISPONIBLE"));
    }

    @Test
    @Order(5)
    @DisplayName("Listar todos los libros")
    void listarLibros() throws Exception {
        mockMvc.perform(get("/api/libros")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].titulo").value("Clean Code"));
    }

    @Test
    @Order(6)
    @DisplayName("Buscar libro por ISBN")
    void buscarPorIsbn() throws Exception {
        mockMvc.perform(get("/api/libros/9780132350884")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.autor").value("Robert Martin"));
    }

    @Test
    @Order(7)
    @DisplayName("Buscar libro por autor")
    void buscarPorAutor() throws Exception {
        mockMvc.perform(get("/api/libros/buscar?autor=robert")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    @Order(8)
    @DisplayName("Actualizar libro")
    void actualizarLibro() throws Exception {
        mockMvc.perform(put("/api/libros/9780132350884")
                        .header("Authorization", "Bearer " + librarianToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"isbn":"9780132350884","titulo":"Clean Code 2nd Ed","autor":"Robert C. Martin","categoria":"Software","añoPub":2008,"editorial":"Prentice Hall"}
                            """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.titulo").value("Clean Code 2nd Ed"));
    }

    @Test
    @Order(9)
    @DisplayName("Usuario sin permisos no puede eliminar libro")
    void eliminarLibroSinPermiso() throws Exception {
        mockMvc.perform(delete("/api/libros/9780132350884")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @Order(10)
    @DisplayName("Libro no encontrado devuelve 404")
    void libroNoEncontrado() throws Exception {
        mockMvc.perform(get("/api/libros/9999999999")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Libro no encontrado: 9999999999"));
    }

    @Test
    @Order(11)
    @DisplayName("Crear segundo libro para préstamos")
    void crearSegundoLibro() throws Exception {
        mockMvc.perform(post("/api/libros")
                        .header("Authorization", "Bearer " + librarianToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"isbn":"9780201633610","titulo":"Design Patterns","autor":"Gang of Four","categoria":"Software","añoPub":1994,"editorial":"Addison-Wesley"}
                            """))
                .andExpect(status().isCreated());
    }

    @Test
    @Order(12)
    @DisplayName("Realizar préstamo de libro")
    void realizarPrestamo() throws Exception {
        mockMvc.perform(post("/api/prestamos")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"usuarioId":2,"libroIsbn":"9780201633610"}
                            """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.estado").value("ACTIVO"));
    }

    @Test
    @Order(13)
    @DisplayName("No permitir préstamo duplicado del mismo libro")
    void prestamoDuplicado() throws Exception {
        mockMvc.perform(post("/api/prestamos")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"usuarioId":2,"libroIsbn":"9780201633610"}
                            """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("El usuario ya tiene un préstamo activo de este libro"));
    }

    @Test
    @Order(14)
    @DisplayName("Consultar historial de préstamos del usuario")
    void historialPrestamos() throws Exception {
        mockMvc.perform(get("/api/prestamos/usuarios/2")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].libro.titulo").value("Design Patterns"));
    }

    @Test
    @Order(15)
    @DisplayName("Devolver libro")
    void devolverLibro() throws Exception {
        mockMvc.perform(put("/api/prestamos/1/devolucion")
                        .header("Authorization", "Bearer " + librarianToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("DEVUELTO"))
                .andExpect(jsonPath("$.fechaEntregaReal").exists());
    }

    @Test
    @Order(16)
    @DisplayName("No permitir devolver préstamo ya devuelto")
    void devolverYaDevuelto() throws Exception {
        mockMvc.perform(put("/api/prestamos/1/devolucion")
                        .header("Authorization", "Bearer " + librarianToken))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("El préstamo ya fue devuelto"));
    }

    @Test
    @Order(17)
    @DisplayName("GET /api/usuarios/{id} omite contrasena, resetToken, resetTokenExpiry")
    void obtenerUsuarioNoExponeCamposSensibles() throws Exception {
        mockMvc.perform(get("/api/usuarios/1")
                        .header("Authorization", "Bearer " + librarianToken))
                .andExpect(status().isOk())
                // Debe exponer estos campos
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.nombre").value("Bibliotecario"))
                .andExpect(jsonPath("$.email").value("biblio@test.com"))
                .andExpect(jsonPath("$.dni").value("11111111"))
                .andExpect(jsonPath("$.telefono").value("+5491111111111"))
                .andExpect(jsonPath("$.rol").value("LIBRARIAN"))
                .andExpect(jsonPath("$.estado").value("ACTIVO"))
                // NO debe exponer estos campos sensibles
                .andExpect(jsonPath("$.contrasena").doesNotExist())
                .andExpect(jsonPath("$.resetToken").doesNotExist())
                .andExpect(jsonPath("$.resetTokenExpiry").doesNotExist());
    }

    @Test
    @Order(18)
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
