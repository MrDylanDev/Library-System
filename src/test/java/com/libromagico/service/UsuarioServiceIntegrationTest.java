package com.libromagico.service;

import com.libromagico.dto.RegisterRequest;
import com.libromagico.exception.OperacionInvalidaException;
import com.libromagico.exception.RecursoNoEncontradoException;
import com.libromagico.model.EstadoUsuario;
import com.libromagico.model.RolUsuario;
import com.libromagico.model.Usuario;
import com.libromagico.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:testtx-usuario;DB_CLOSE_DELAY=-1"
})
@ActiveProfiles("test")
class UsuarioServiceIntegrationTest {

    @Autowired
    private UsuarioService usuarioService;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @SpyBean
    private UsuarioRepository spyUsuarioRepository;

    @BeforeEach
    void setUp() {
        usuarioRepository.deleteAll();
    }

    @Test
    @DisplayName("register creates a new user with encoded password and default role USER")
    void register_success() {
        var request = new RegisterRequest(
                "Nuevo Usuario",
                "nuevo@test.com",
                "Password123!",
                "12345678",
                "+5491111111111"
        );

        var result = usuarioService.register(request);

        assertNotNull(result);
        assertNotNull(result.getId());
        assertEquals("Nuevo Usuario", result.getNombre());
        assertEquals("nuevo@test.com", result.getEmail());
        assertEquals("12345678", result.getDni());
        assertEquals("+5491111111111", result.getTelefono());
        assertEquals(RolUsuario.USER, result.getRol());

        // Password must be encoded (different from raw)
        assertNotEquals("Password123!", result.getContrasena());
        assertTrue(passwordEncoder.matches("Password123!", result.getContrasena()),
                "Password should be encoded and match the raw password");

        // User should be persisted
        assertTrue(usuarioRepository.existsByEmail("nuevo@test.com"));
    }

    @Test
    @DisplayName("register throws OperacionInvalidaException for duplicate email")
    void register_duplicateEmail() {
        var existing = new Usuario();
        existing.setNombre("Existing");
        existing.setEmail("existing@test.com");
        existing.setContrasena(passwordEncoder.encode("pass123"));
        existing.setDni("87654321");
        usuarioRepository.save(existing);

        var request = new RegisterRequest(
                "Duplicado",
                "existing@test.com",
                "OtherPass1!",
                "11111111",
                "+5491122334455"
        );

        var ex = assertThrows(OperacionInvalidaException.class,
                () -> usuarioService.register(request));
        assertTrue(ex.getMessage().contains("email"));
    }

    @Test
    @DisplayName("register throws OperacionInvalidaException for duplicate DNI")
    void register_duplicateDni() {
        var existing = new Usuario();
        existing.setNombre("Existing");
        existing.setEmail("existing2@test.com");
        existing.setContrasena(passwordEncoder.encode("pass123"));
        existing.setDni("99999999");
        usuarioRepository.save(existing);

        var request = new RegisterRequest(
                "Duplicado DNI",
                "otro@test.com",
                "OtherPass1!",
                "99999999",
                "+5491122334455"
        );

        var ex = assertThrows(OperacionInvalidaException.class,
                () -> usuarioService.register(request));
        assertTrue(ex.getMessage().contains("DNI"));
    }

    @Test
    @DisplayName("register rolls back on repository failure")
    void register_rollbackOnFailure() {
        doThrow(new RuntimeException("DB failure"))
                .when(spyUsuarioRepository)
                .save(any(Usuario.class));

        var request = new RegisterRequest(
                "Rollback User",
                "rollback@test.com",
                "Pass1234!",
                "55555555",
                "+5491133334444"
        );

        assertThrows(RuntimeException.class,
                () -> usuarioService.register(request));

        assertFalse(usuarioRepository.existsByEmail("rollback@test.com"),
                "No user should be persisted after rollback");
    }

    @Test
    @DisplayName("forgotPassword sets reset token and expiry for existing email")
    void forgotPassword_success() {
        var user = createUser("forgot@test.com", "33333333");

        var result = usuarioService.forgotPassword("forgot@test.com");

        assertTrue(result.isPresent());
        assertEquals(user.getId(), result.get().getId());
        assertNotNull(result.get().getResetToken());
        assertNotNull(result.get().getResetTokenExpiry());
        assertTrue(result.get().getResetTokenExpiry().isAfter(LocalDateTime.now()));
    }

    @Test
    @DisplayName("forgotPassword returns Optional.empty() for unknown email")
    void forgotPassword_unknownEmail() {
        var result = usuarioService.forgotPassword("unknown@test.com");

        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("resetPassword updates password and clears token for valid token")
    void resetPassword_success() {
        var user = createUser("reset@test.com", "44444444");
        user.setResetToken("valid-token-123");
        user.setResetTokenExpiry(LocalDateTime.now().plusHours(1));
        usuarioRepository.save(user);

        usuarioService.resetPassword("valid-token-123", "NewPass123!");

        var updated = usuarioRepository.findByEmail("reset@test.com").orElseThrow();
        assertTrue(passwordEncoder.matches("NewPass123!", updated.getContrasena()),
                "Password should be updated and encoded");
        assertNull(updated.getResetToken());
        assertNull(updated.getResetTokenExpiry());
    }

    @Test
    @DisplayName("resetPassword throws OperacionInvalidaException for invalid token")
    void resetPassword_invalidToken() {
        assertThrows(OperacionInvalidaException.class,
                () -> usuarioService.resetPassword("nonexistent-token", "NewPass123!"));
    }

    @Test
    @DisplayName("resetPassword throws OperacionInvalidaException for expired token")
    void resetPassword_expiredToken() {
        var user = createUser("expired@test.com", "55555555");
        user.setResetToken("expired-token");
        user.setResetTokenExpiry(LocalDateTime.now().minusHours(1));
        usuarioRepository.save(user);

        assertThrows(OperacionInvalidaException.class,
                () -> usuarioService.resetPassword("expired-token", "NewPass123!"));
    }

    @Test
    @DisplayName("actualizarRol updates user role")
    void actualizarRol_success() {
        var user = createUser("rol@test.com", "66666666");

        var updated = usuarioService.actualizarRol(user.getId(), RolUsuario.LIBRARIAN);

        assertEquals(RolUsuario.LIBRARIAN, updated.getRol());
        assertEquals(user.getId(), updated.getId());
    }

    @Test
    @DisplayName("actualizarEstado updates user status")
    void actualizarEstado_success() {
        var user = createUser("estado@test.com", "77777777");

        var updated = usuarioService.actualizarEstado(user.getId(), EstadoUsuario.BLOQUEADO);

        assertEquals(EstadoUsuario.BLOQUEADO, updated.getEstado());
        assertEquals(user.getId(), updated.getId());
    }

    @Test
    @DisplayName("actualizarRol throws RecursoNoEncontradoException for unknown user")
    void actualizarRol_notFound() {
        assertThrows(RecursoNoEncontradoException.class,
                () -> usuarioService.actualizarRol(999L, RolUsuario.ADMIN));
    }

    @Test
    @DisplayName("actualizarEstado throws RecursoNoEncontradoException for unknown user")
    void actualizarEstado_notFound() {
        assertThrows(RecursoNoEncontradoException.class,
                () -> usuarioService.actualizarEstado(999L, EstadoUsuario.BLOQUEADO));
    }

    @Test
    @DisplayName("buscarPorId returns user by ID")
    void buscarPorId_success() {
        var user = createUser("findbyid@test.com", "88888888");

        var found = usuarioService.buscarPorId(user.getId());

        assertNotNull(found);
        assertEquals(user.getId(), found.getId());
        assertEquals("findbyid@test.com", found.getEmail());
    }

    @Test
    @DisplayName("buscarPorId throws RecursoNoEncontradoException for unknown ID")
    void buscarPorId_notFound() {
        assertThrows(RecursoNoEncontradoException.class,
                () -> usuarioService.buscarPorId(999L));
    }

    @Test
    @DisplayName("listarTodos returns all users")
    void listarTodos() {
        createUser("list1@test.com", "12121212");
        createUser("list2@test.com", "34343434");

        var result = usuarioService.listarTodos();

        assertEquals(2, result.size());
    }

    private Usuario createUser(String email, String dni) {
        var user = new Usuario();
        user.setNombre("Test User");
        user.setEmail(email);
        user.setContrasena(passwordEncoder.encode("pass123"));
        user.setDni(dni);
        user.setRol(RolUsuario.USER);
        user.setEstado(EstadoUsuario.ACTIVO);
        return usuarioRepository.save(user);
    }
}
