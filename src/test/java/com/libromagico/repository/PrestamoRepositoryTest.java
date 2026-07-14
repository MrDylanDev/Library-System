package com.libromagico.repository;

import com.libromagico.model.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
class PrestamoRepositoryTest {

    @Autowired
    private PrestamoRepository prestamoRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private LibroRepository libroRepository;

    @Test
    @DisplayName("existsByUsuarioAndLibroAndEstado returns true when ACTIVO loan exists")
    void existsByUsuarioAndLibroAndEstado_activoLoanExists_returnsTrue() {
        var usuario = createUser();
        var libro = createBook();
        createLoan(usuario, libro, EstadoPrestamo.ACTIVO);

        assertTrue(prestamoRepository.existsByUsuarioAndLibroAndEstado(
                usuario, libro, EstadoPrestamo.ACTIVO));
    }

    @Test
    @DisplayName("existsByUsuarioAndLibroAndEstado returns false when only DEVUELTO loans exist")
    void existsByUsuarioAndLibroAndEstado_onlyDevueltoLoan_returnsFalseForActivo() {
        var usuario = createUser();
        var libro = createBook();
        createLoan(usuario, libro, EstadoPrestamo.DEVUELTO);

        assertFalse(prestamoRepository.existsByUsuarioAndLibroAndEstado(
                usuario, libro, EstadoPrestamo.ACTIVO));
    }

    @Test
    @DisplayName("existsByUsuarioAndLibroAndEstado returns true when both ACTIVO and DEVUELTO exist")
    void existsByUsuarioAndLibroAndEstado_bothActivoAndDevuelto_returnsTrueForActivo() {
        var usuario = createUser();
        var libro = createBook();
        createLoan(usuario, libro, EstadoPrestamo.ACTIVO);
        createLoan(usuario, libro, EstadoPrestamo.DEVUELTO);

        assertTrue(prestamoRepository.existsByUsuarioAndLibroAndEstado(
                usuario, libro, EstadoPrestamo.ACTIVO));
    }

    @Test
    @DisplayName("existsByLibroAndEstado returns true when ACTIVO loan exists for libro")
    void existsByLibroAndEstado_activoLoanExists_returnsTrue() {
        var usuario = createUser();
        var libro = createBook();
        createLoan(usuario, libro, EstadoPrestamo.ACTIVO);

        assertTrue(prestamoRepository.existsByLibroAndEstado(libro, EstadoPrestamo.ACTIVO));
    }

    @Test
    @DisplayName("existsByLibroAndEstado returns false when no loans exist for libro")
    void existsByLibroAndEstado_noLoans_returnsFalse() {
        var libro = createBook();

        assertFalse(prestamoRepository.existsByLibroAndEstado(libro, EstadoPrestamo.ACTIVO));
    }

    private Usuario createUser() {
        var user = new Usuario();
        user.setNombre("Test User");
        user.setEmail("test@example.com");
        user.setContrasena("password123");
        user.setDni("12345678");
        user.setTelefono("+5491111111111");
        return usuarioRepository.save(user);
    }

    private Libro createBook() {
        var book = new Libro();
        book.setIsbn("9780132350884");
        book.setTitulo("Test Book");
        book.setAutor("Test Author");
        book.setCopiasDisponibles(1);
        return libroRepository.save(book);
    }

    private Prestamo createLoan(Usuario usuario, Libro libro, EstadoPrestamo estado) {
        var loan = new Prestamo();
        loan.setUsuario(usuario);
        loan.setLibro(libro);
        loan.setFechaPrestamo(LocalDate.now());
        loan.setFechaDevolucion(LocalDate.now().plusDays(15));
        loan.setEstado(estado);
        return prestamoRepository.save(loan);
    }
}
