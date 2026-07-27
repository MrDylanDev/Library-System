package com.libromagico.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.libromagico.dto.AuthResponse;
import com.libromagico.model.*;
import com.libromagico.repository.*;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:testtx-admin;DB_CLOSE_DELAY=-1"
})
class AdminControllerIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
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

    private AuthResponse login(String email, String password) throws Exception {
        var response = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"email":"%s","contrasena":"%s"}
                            """.formatted(email, password)))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readValue(response.getResponse().getContentAsString(), AuthResponse.class);
    }

    private Usuario createUser(String email, String dni, RolUsuario rol, String password) {
        var user = new Usuario();
        user.setNombre("Usuario " + rol.name());
        user.setEmail(email);
        user.setContrasena(passwordEncoder.encode(password));
        user.setDni(dni);
        user.setTelefono("+5491122334455");
        user.setRol(rol);
        return usuarioRepository.save(user);
    }

    private AuthResponse createAdminAndLogin() throws Exception {
        String email = uniqueEmail("admin");
        createUser(email, "99999999", RolUsuario.ADMIN, "Admin123!");
        return login(email, "Admin123!");
    }

    private AuthResponse createUserAndLogin() throws Exception {
        String email = uniqueEmail("user");
        createUser(email, "88888888", RolUsuario.USER, "User123!");
        return login(email, "User123!");
    }

    private AuthResponse createLibrarianAndLogin() throws Exception {
        String email = uniqueEmail("biblio");
        createUser(email, "77777777", RolUsuario.LIBRARIAN, "Biblio123!");
        return login(email, "Biblio123!");
    }

    private Multa createMulta(Usuario usuario) {
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
        prestamo.setFechaPrestamo(LocalDate.now().minusDays(20));
        prestamo.setFechaDevolucion(LocalDate.now().minusDays(5));
        prestamo.setEstado(EstadoPrestamo.ACTIVO);
        prestamo = prestamoRepository.save(prestamo);

        var multa = new Multa();
        multa.setPrestamo(prestamo);
        multa.setMonto(new BigDecimal("10.00"));
        multa.setEstado(EstadoMulta.PENDIENTE);
        return multaRepository.save(multa);
    }

    @Test
    @DisplayName("PUT /api/admin/usuarios/{id}/rol - ADMIN cambia rol de usuario a LIBRARIAN")
    void adminCambiaRol() throws Exception {
        var admin = createAdminAndLogin();
        var targetUser = createUser(uniqueEmail("target"), "11111111", RolUsuario.USER, "Pass123!");

        mockMvc.perform(put("/api/admin/usuarios/" + targetUser.getId() + "/rol")
                        .header("Authorization", "Bearer " + admin.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"rol":"LIBRARIAN"}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rol").value("LIBRARIAN"))
                .andExpect(jsonPath("$.id").value(targetUser.getId()));
    }

    @Test
    @DisplayName("PUT /api/admin/usuarios/{id}/estado - ADMIN cambia estado de usuario a BLOQUEADO")
    void adminCambiaEstado() throws Exception {
        var admin = createAdminAndLogin();
        var targetUser = createUser(uniqueEmail("target"), "22222222", RolUsuario.USER, "Pass123!");

        mockMvc.perform(put("/api/admin/usuarios/" + targetUser.getId() + "/estado")
                        .header("Authorization", "Bearer " + admin.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"estado":"BLOQUEADO"}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("BLOQUEADO"))
                .andExpect(jsonPath("$.id").value(targetUser.getId()));
    }

    @Test
    @DisplayName("GET /api/admin/multas - ADMIN lista todas las multas")
    void adminListaMultas() throws Exception {
        var admin = createAdminAndLogin();
        var user = createUser(uniqueEmail("multauser"), "33333333", RolUsuario.USER, "Pass123!");
        createMulta(user);

        mockMvc.perform(get("/api/admin/multas")
                        .header("Authorization", "Bearer " + admin.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1));
    }

    @Test
    @DisplayName("PUT /api/admin/multas/{id}/pagar - ADMIN paga una multa")
    void adminPagaMulta() throws Exception {
        var admin = createAdminAndLogin();
        var user = createUser(uniqueEmail("multauser2"), "44444444", RolUsuario.USER, "Pass123!");
        var multa = createMulta(user);

        mockMvc.perform(put("/api/admin/multas/" + multa.getId() + "/pagar")
                        .header("Authorization", "Bearer " + admin.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("PAGADO"));
    }

    @Test
    @DisplayName("PUT /api/admin/usuarios/{id}/rol - USER obtiene 403")
    void userNoPuedeCambiarRol() throws Exception {
        var user = createUserAndLogin();
        var targetUser = createUser(uniqueEmail("target2"), "55555555", RolUsuario.USER, "Pass123!");

        mockMvc.perform(put("/api/admin/usuarios/" + targetUser.getId() + "/rol")
                        .header("Authorization", "Bearer " + user.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"rol":"LIBRARIAN"}"""))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("PUT /api/admin/usuarios/{id}/rol - LIBRARIAN obtiene 403")
    void librarianNoPuedeCambiarRol() throws Exception {
        var librarian = createLibrarianAndLogin();
        var targetUser = createUser(uniqueEmail("target3"), "66666666", RolUsuario.USER, "Pass123!");

        mockMvc.perform(put("/api/admin/usuarios/" + targetUser.getId() + "/rol")
                        .header("Authorization", "Bearer " + librarian.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"rol":"LIBRARIAN"}"""))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /api/admin/multas - USER obtiene 403")
    void userNoPuedeListarMultas() throws Exception {
        var user = createUserAndLogin();

        mockMvc.perform(get("/api/admin/multas")
                        .header("Authorization", "Bearer " + user.token()))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /api/admin/multas - LIBRARIAN obtiene 403")
    void librarianNoPuedeListarMultas() throws Exception {
        var librarian = createLibrarianAndLogin();

        mockMvc.perform(get("/api/admin/multas")
                        .header("Authorization", "Bearer " + librarian.token()))
                .andExpect(status().isForbidden());
    }

    private Prestamo createPrestamo(Usuario usuario, EstadoPrestamo estado) {
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
        prestamo.setEstado(estado);
        return prestamoRepository.save(prestamo);
    }

    @Test
    @DisplayName("GET /api/admin/prestamos - ADMIN lista todos los préstamos")
    void adminListaPrestamos() throws Exception {
        var admin = createAdminAndLogin();
        var user = createUser(uniqueEmail("prestamouser"), "55550111", RolUsuario.USER, "Pass123!");
        createPrestamo(user, EstadoPrestamo.ACTIVO);
        createPrestamo(user, EstadoPrestamo.DEVUELTO);

        mockMvc.perform(get("/api/admin/prestamos")
                        .header("Authorization", "Bearer " + admin.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2));
    }

    @Test
    @DisplayName("GET /api/admin/prestamos - LIBRARIAN lista todos los préstamos")
    void librarianListaPrestamos() throws Exception {
        var librarian = createLibrarianAndLogin();
        var user = createUser(uniqueEmail("prestamouser2"), "55550112", RolUsuario.USER, "Pass123!");
        createPrestamo(user, EstadoPrestamo.ACTIVO);

        mockMvc.perform(get("/api/admin/prestamos")
                        .header("Authorization", "Bearer " + librarian.token()))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /api/admin/prestamos - USER obtiene 403")
    void userNoPuedeListarPrestamos() throws Exception {
        var user = createUserAndLogin();

        mockMvc.perform(get("/api/admin/prestamos")
                        .header("Authorization", "Bearer " + user.token()))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /api/admin/prestamos - Unauthenticated obtiene 401")
    void unauthenticatedNoPuedeListarPrestamos() throws Exception {
        mockMvc.perform(get("/api/admin/prestamos"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /api/admin/prestamos?estado=DEVUELTO - filtra por estado")
    void adminListaPrestamosFiltrados() throws Exception {
        var admin = createAdminAndLogin();
        var user = createUser(uniqueEmail("prestamouser3"), "55550113", RolUsuario.USER, "Pass123!");
        createPrestamo(user, EstadoPrestamo.ACTIVO);
        createPrestamo(user, EstadoPrestamo.DEVUELTO);

        mockMvc.perform(get("/api/admin/prestamos?estado=DEVUELTO")
                        .header("Authorization", "Bearer " + admin.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].estado").value("DEVUELTO"));
    }

    @Test
    @DisplayName("GET /api/admin/prestamos?estado=ATRASADO - estado sin resultados devuelve página vacía")
    void adminListaPrestamosEstadoVacio() throws Exception {
        var admin = createAdminAndLogin();
        var user = createUser(uniqueEmail("prestamouser4"), "55550114", RolUsuario.USER, "Pass123!");
        createPrestamo(user, EstadoPrestamo.DEVUELTO);

        mockMvc.perform(get("/api/admin/prestamos?estado=ATRASADO")
                        .header("Authorization", "Bearer " + admin.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(0));
    }

    @Test
    @DisplayName("GET /api/admin/prestamos?estado=INVALIDO - estado inválido devuelve 400")
    void adminListaPrestamosEstadoInvalido() throws Exception {
        var admin = createAdminAndLogin();

        mockMvc.perform(get("/api/admin/prestamos?estado=INVALIDO")
                        .header("Authorization", "Bearer " + admin.token()))
                .andExpect(status().isBadRequest());
    }
}
