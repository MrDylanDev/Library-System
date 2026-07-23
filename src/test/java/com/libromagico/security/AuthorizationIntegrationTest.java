package com.libromagico.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.libromagico.dto.AuthResponse;
import com.libromagico.model.*;
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

import java.time.LocalDate;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:testtx-authz;DB_CLOSE_DELAY=-1"
})
class AuthorizationIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private LibroRepository libroRepository;
    @Autowired private PrestamoRepository prestamoRepository;

    private String userToken;
    private String librarianToken;
    private String adminToken;
    private String bookIsbn;
    private Long userId;

    @BeforeEach
    void setUp() throws Exception {
        prestamoRepository.deleteAll();
        libroRepository.deleteAll();
        usuarioRepository.deleteAll();

        userToken = createUserAndLogin();
        librarianToken = createLibrarianAndLogin();
        adminToken = createAdminAndLogin();
        bookIsbn = createBook(librarianToken);
        userId = getUserIdFromToken(userToken);
    }

    private String uniqueEmail(String prefix) {
        return prefix + "." + UUID.randomUUID().toString().substring(0, 8) + "@test.com";
    }

    private Long getUserIdFromToken(String token) throws Exception {
        var response = mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        var map = objectMapper.readValue(response.getResponse().getContentAsString(), java.util.Map.class);
        return ((Number) map.get("id")).longValue();
    }

    private void createAndSaveUser(String email, String dni, RolUsuario rol, String password) {
        var user = new Usuario();
        user.setNombre("User " + rol.name());
        user.setEmail(email);
        user.setContrasena(passwordEncoder.encode(password));
        user.setDni(dni);
        user.setTelefono("+5491122334455");
        user.setRol(rol);
        usuarioRepository.save(user);
    }

    private String createUserAndLogin() throws Exception {
        String email = uniqueEmail("user");
        createAndSaveUser(email, "11111111", RolUsuario.USER, "User123!");
        var response = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"email":"%s","contrasena":"User123!"}
                            """.formatted(email)))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readValue(response.getResponse().getContentAsString(), AuthResponse.class).token();
    }

    private String createLibrarianAndLogin() throws Exception {
        String email = uniqueEmail("biblio");
        createAndSaveUser(email, "22222222", RolUsuario.LIBRARIAN, "Biblio123!");
        var response = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"email":"%s","contrasena":"Biblio123!"}
                            """.formatted(email)))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readValue(response.getResponse().getContentAsString(), AuthResponse.class).token();
    }

    private String createAdminAndLogin() throws Exception {
        String email = uniqueEmail("admin");
        createAndSaveUser(email, "33333333", RolUsuario.ADMIN, "Admin123!");
        var response = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"email":"%s","contrasena":"Admin123!"}
                            """.formatted(email)))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readValue(response.getResponse().getContentAsString(), AuthResponse.class).token();
    }

    private String createBook(String token) throws Exception {
        var isbn = "9780132350884";
        mockMvc.perform(post("/api/libros")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"isbn":"%s","titulo":"Clean Code","autor":"Robert Martin","categoria":"Software","añoPub":2008,"editorial":"Prentice Hall"}
                            """.formatted(isbn)))
                .andExpect(status().isCreated());
        return isbn;
    }

    @Test
    @DisplayName("USER: GET /api/libros -> 200")
    void userGetLibros() throws Exception {
        mockMvc.perform(get("/api/libros")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("USER: POST /api/libros -> 403")
    void userPostLibros() throws Exception {
        mockMvc.perform(post("/api/libros")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"isbn":"9780201633610","titulo":"Design Patterns","autor":"Gang of Four","categoria":"Software","añoPub":1994,"editorial":"Addison-Wesley"}
                            """))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("USER: PUT /api/libros/{isbn} -> 403")
    void userPutLibros() throws Exception {
        mockMvc.perform(put("/api/libros/" + bookIsbn)
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"isbn":"%s","titulo":"Clean Code","autor":"Robert Martin","categoria":"Software","añoPub":2008,"editorial":"Prentice Hall"}
                            """.formatted(bookIsbn)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("USER: DELETE /api/libros/{isbn} -> 403")
    void userDeleteLibros() throws Exception {
        mockMvc.perform(delete("/api/libros/" + bookIsbn)
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("USER: GET /api/usuarios -> 403")
    void userGetUsuarios() throws Exception {
        mockMvc.perform(get("/api/usuarios")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("USER: POST /api/prestamos -> 200")
    void userPostPrestamos() throws Exception {
        mockMvc.perform(post("/api/prestamos")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"usuarioId":%s,"libroIsbn":"%s"}
                            """.formatted(userId, bookIsbn)))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("USER: GET /api/prestamos/usuarios/{id} -> 200 (propios préstamos)")
    void userGetPrestamosUsuario() throws Exception {
        mockMvc.perform(get("/api/prestamos/usuarios/" + userId)
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("USER: PUT /api/admin/** -> 403")
    void userPutAdmin() throws Exception {
        mockMvc.perform(put("/api/admin/usuarios/1/rol")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"rol":"LIBRARIAN"}"""))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("LIBRARIAN: GET /api/libros -> 200")
    void librarianGetLibros() throws Exception {
        mockMvc.perform(get("/api/libros")
                        .header("Authorization", "Bearer " + librarianToken))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("LIBRARIAN: POST /api/libros -> 200")
    void librarianPostLibros() throws Exception {
        mockMvc.perform(post("/api/libros")
                        .header("Authorization", "Bearer " + librarianToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"isbn":"9780201633610","titulo":"Design Patterns","autor":"Gang of Four","categoria":"Software","añoPub":1994,"editorial":"Addison-Wesley"}
                            """))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("LIBRARIAN: PUT /api/libros/{isbn} -> 200")
    void librarianPutLibros() throws Exception {
        mockMvc.perform(put("/api/libros/" + bookIsbn)
                        .header("Authorization", "Bearer " + librarianToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"isbn":"%s","titulo":"Clean Code 2da Ed","autor":"Robert C. Martin","categoria":"Software","añoPub":2008,"editorial":"Prentice Hall"}
                            """.formatted(bookIsbn)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("LIBRARIAN: DELETE /api/libros/{isbn} -> 204")
    void librarianDeleteLibros() throws Exception {
        var isbn = "9780201633610";
        mockMvc.perform(post("/api/libros")
                        .header("Authorization", "Bearer " + librarianToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"isbn":"%s","titulo":"Design Patterns","autor":"Gang of Four","categoria":"Software","añoPub":1994,"editorial":"Addison-Wesley"}
                            """.formatted(isbn)))
                .andExpect(status().isCreated());

        mockMvc.perform(delete("/api/libros/" + isbn)
                        .header("Authorization", "Bearer " + librarianToken))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("LIBRARIAN: GET /api/usuarios -> 200")
    void librarianGetUsuarios() throws Exception {
        mockMvc.perform(get("/api/usuarios")
                        .header("Authorization", "Bearer " + librarianToken))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("LIBRARIAN: PUT /api/admin/** -> 403")
    void librarianPutAdmin() throws Exception {
        mockMvc.perform(put("/api/admin/usuarios/1/rol")
                        .header("Authorization", "Bearer " + librarianToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"rol":"LIBRARIAN"}"""))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("LIBRARIAN: POST /api/prestamos -> 200")
    void librarianPostPrestamos() throws Exception {
        mockMvc.perform(post("/api/prestamos")
                        .header("Authorization", "Bearer " + librarianToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"usuarioId":%s,"libroIsbn":"%s"}
                            """.formatted(userId, bookIsbn)))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("LIBRARIAN: PUT /api/prestamos/{id}/devolucion -> 200")
    void librarianPutDevolucion() throws Exception {
        var prestamoResponse = mockMvc.perform(post("/api/prestamos")
                        .header("Authorization", "Bearer " + librarianToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"usuarioId":%s,"libroIsbn":"%s"}
                            """.formatted(userId, bookIsbn)))
                .andExpect(status().isCreated())
                .andReturn();
        var prestamoBody = objectMapper.readValue(prestamoResponse.getResponse().getContentAsString(), java.util.Map.class);
        var prestamoId = ((Number) prestamoBody.get("id")).longValue();

        mockMvc.perform(put("/api/prestamos/" + prestamoId + "/devolucion")
                        .header("Authorization", "Bearer " + librarianToken))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("ADMIN: PUT /api/admin/usuarios/{id}/rol -> 200")
    void adminPutRol() throws Exception {
        mockMvc.perform(put("/api/admin/usuarios/" + userId + "/rol")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"rol":"LIBRARIAN"}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rol").value("LIBRARIAN"));
    }

    @Test
    @DisplayName("ADMIN: PUT /api/admin/usuarios/{id}/estado -> 200")
    void adminPutEstado() throws Exception {
        mockMvc.perform(put("/api/admin/usuarios/" + userId + "/estado")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"estado":"BLOQUEADO"}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("BLOQUEADO"));
    }

    @Test
    @DisplayName("ADMIN: GET /api/admin/multas -> 200")
    void adminGetMultas() throws Exception {
        mockMvc.perform(get("/api/admin/multas")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("ADMIN: PUT /api/admin/multas/{id}/pagar -> 404 (multa no existe)")
    void adminPutMultasPagar() throws Exception {
        mockMvc.perform(put("/api/admin/multas/999/pagar")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("ADMIN: GET /api/libros -> 200")
    void adminGetLibros() throws Exception {
        mockMvc.perform(get("/api/libros")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
    }
}
