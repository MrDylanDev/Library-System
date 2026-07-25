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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:testtx-multa-ctrl;DB_CLOSE_DELAY=-1"
})
class MultaControllerIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private LibroRepository libroRepository;
    @Autowired private PrestamoRepository prestamoRepository;
    @Autowired private MultaRepository multaRepository;

    private String userEmail;
    private String userPassword = "User123!";

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

    private String createUserAndLogin() throws Exception {
        userEmail = uniqueEmail("user");
        var user = new Usuario();
        user.setNombre("User Test");
        user.setEmail(userEmail);
        user.setContrasena(passwordEncoder.encode(userPassword));
        user.setDni("11111111");
        user.setTelefono("+5491122334455");
        user.setRol(RolUsuario.USER);
        usuarioRepository.save(user);

        var response = mockMvc.perform(post("/api/auth/login")
                        .contentType("application/json")
                        .content("""
                            {"email":"%s","contrasena":"%s"}
                            """.formatted(userEmail, userPassword)))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readValue(response.getResponse().getContentAsString(), AuthResponse.class).token();
    }

    private void createMultasParaUsuario(Long usuarioId, int pendientes, int pagadas) {
        var libro = new Libro();
        libro.setIsbn("9780451524934");
        libro.setTitulo("1984");
        libro.setAutor("George Orwell");
        libro.setCopiasDisponibles(1);
        libro.setEstado(EstadoLibro.DISPONIBLE);
        libroRepository.save(libro);

        var usuario = usuarioRepository.findById(usuarioId).orElseThrow();

        for (int i = 0; i < pendientes; i++) {
            var prestamo = new Prestamo();
            prestamo.setUsuario(usuario);
            prestamo.setLibro(libro);
            prestamo.setFechaPrestamo(LocalDate.now().minusDays(20 + i));
            prestamo.setFechaDevolucion(LocalDate.now().minusDays(5 + i));
            prestamo.setEstado(EstadoPrestamo.ACTIVO);
            prestamo = prestamoRepository.save(prestamo);

            var multa = new Multa();
            multa.setPrestamo(prestamo);
            multa.setMonto(new BigDecimal("10.00"));
            multa.setEstado(EstadoMulta.PENDIENTE);
            multaRepository.save(multa);
        }

        for (int i = 0; i < pagadas; i++) {
            var prestamo = new Prestamo();
            prestamo.setUsuario(usuario);
            prestamo.setLibro(libro);
            prestamo.setFechaPrestamo(LocalDate.now().minusDays(40 + i));
            prestamo.setFechaDevolucion(LocalDate.now().minusDays(25 + i));
            prestamo.setEstado(EstadoPrestamo.DEVUELTO);
            prestamo = prestamoRepository.save(prestamo);

            var multa = new Multa();
            multa.setPrestamo(prestamo);
            multa.setMonto(new BigDecimal("5.00"));
            multa.setEstado(EstadoMulta.PAGADO);
            multaRepository.save(multa);
        }
    }

    @Test
    @DisplayName("GET /api/multas/mis-multas - 200 with paginated PENDIENTE multas")
    void misMultas_paginated() throws Exception {
        var token = createUserAndLogin();
        var user = usuarioRepository.findByEmail(userEmail).orElseThrow();
        createMultasParaUsuario(user.getId(), 10, 3);

        mockMvc.perform(get("/api/multas/mis-multas?page=0&size=5")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(5))
                .andExpect(jsonPath("$.totalElements").value(10))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.content[0].estado").value("PENDIENTE"))
                .andExpect(jsonPath("$.content[0].id").isNumber())
                .andExpect(jsonPath("$.content[0].monto").isNumber())
                .andExpect(jsonPath("$.content[0].prestamoId").isNumber())
                .andExpect(jsonPath("$.content[0].libroTitulo").value("1984"))
                .andExpect(jsonPath("$.content[0].libroIsbn").value("9780451524934"))
                .andExpect(jsonPath("$.content[0].fechaPrestamo").isString())
                .andExpect(jsonPath("$.content[0].fechaDevolucion").isString());
    }

    @Test
    @DisplayName("GET /api/multas/mis-multas - 200 with totalElements = 0 when no multas")
    void misMultas_empty() throws Exception {
        var token = createUserAndLogin();

        mockMvc.perform(get("/api/multas/mis-multas")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(0))
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    @DisplayName("GET /api/multas/mis-multas - 401 when unauthenticated")
    void misMultas_unauthenticated() throws Exception {
        mockMvc.perform(get("/api/multas/mis-multas"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /api/multas/mis-multas - 200 with page 1 and only PENDIENTE multas")
    void misMultas_secondPage_onlyPendiente() throws Exception {
        var token = createUserAndLogin();
        var user = usuarioRepository.findByEmail(userEmail).orElseThrow();
        // 10 PENDIENTE + 3 PAGADO — only PENDIENTE should appear in results
        createMultasParaUsuario(user.getId(), 10, 3);

        // Verify pagination across pages — page 1 should have remaining 5
        mockMvc.perform(get("/api/multas/mis-multas?page=1&size=5")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(5))
                .andExpect(jsonPath("$.totalElements").value(10))
                .andExpect(jsonPath("$.number").value(1))
                .andExpect(jsonPath("$.content[0].estado").value("PENDIENTE"))
                .andExpect(jsonPath("$.content[4].estado").value("PENDIENTE"));
    }

    @Test
    @DisplayName("GET /api/multas/mis-multas - 200 other user's multas not leaked")
    void misMultas_otherUserNotLeaked() throws Exception {
        var token = createUserAndLogin();
        var user = usuarioRepository.findByEmail(userEmail).orElseThrow();
        createMultasParaUsuario(user.getId(), 5, 0);

        // Create another user with their own multas
        String otherEmail = uniqueEmail("other");
        var otherUser = new Usuario();
        otherUser.setNombre("Other User");
        otherUser.setEmail(otherEmail);
        otherUser.setContrasena(passwordEncoder.encode("Other123!"));
        otherUser.setDni("87654321");
        otherUser.setRol(RolUsuario.USER);
        otherUser = usuarioRepository.save(otherUser);
        createMultasParaUsuario(otherUser.getId(), 7, 0);

        // Authenticated user should only see THEIR multas, not the other user's
        mockMvc.perform(get("/api/multas/mis-multas?page=0&size=20")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(5));
    }

    @Test
    @DisplayName("GET /api/multas/mis-multas - 404 when SecurityContext email not found in DB")
    void misMultas_emailNotFound() throws Exception {
        // Use Spring Security Test's request post-processor to bypass JWT filter
        // and directly set an authenticated user whose email does not exist in DB.
        // This tests the controller's defensive code path:
        // @AuthenticationPrincipal → UsuarioRepository.findByEmail → 404
        mockMvc.perform(get("/api/multas/mis-multas")
                        .with(user("nonexistent@test.com").roles("USER")))
                .andExpect(status().isNotFound());
    }
}
