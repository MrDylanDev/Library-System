package com.libromagico.service;

import com.libromagico.exception.OperacionInvalidaException;
import com.libromagico.model.*;
import com.libromagico.repository.LibroRepository;
import com.libromagico.repository.PrestamoRepository;
import com.libromagico.repository.UsuarioRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:testtx-libroservice;DB_CLOSE_DELAY=-1"
})
@ActiveProfiles("test")
class LibroServiceSafeDeleteTest {

    @Autowired
    private LibroService libroService;

    @Autowired
    private LibroRepository libroRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PrestamoRepository prestamoRepository;

    @Test
    @DisplayName("eliminar() throws OperacionInvalidaException when active loans exist")
    void eliminar_throwsWhenActiveLoansExist() {
        var user = new Usuario();
        user.setNombre("Test User");
        user.setEmail("txdel@test.com");
        user.setContrasena("pass123");
        user.setDni("87654321");
        user.setTelefono("+5491111111111");
        usuarioRepository.save(user);

        var libro = new Libro();
        libro.setIsbn("9780132350884");
        libro.setTitulo("Clean Code");
        libro.setAutor("Robert Martin");
        libro.setCopiasDisponibles(1);
        libro.setEstado(EstadoLibro.DISPONIBLE);
        libroRepository.save(libro);

        var prestamo = new Prestamo();
        prestamo.setUsuario(user);
        prestamo.setLibro(libro);
        prestamo.setFechaPrestamo(LocalDate.now());
        prestamo.setFechaDevolucion(LocalDate.now().plusDays(15));
        prestamo.setEstado(EstadoPrestamo.ACTIVO);
        prestamoRepository.save(prestamo);

        assertThrows(OperacionInvalidaException.class,
                () -> libroService.eliminar("9780132350884"));

        assertTrue(libroRepository.findById("9780132350884").isPresent(),
                "Book should NOT have been deleted");
    }

    @Test
    @DisplayName("eliminar() succeeds when no active loans exist")
    void eliminar_succeedsWhenNoActiveLoans() {
        var libro = new Libro();
        libro.setIsbn("9780201633610");
        libro.setTitulo("Design Patterns");
        libro.setAutor("Gang of Four");
        libro.setCopiasDisponibles(1);
        libro.setEstado(EstadoLibro.DISPONIBLE);
        libroRepository.save(libro);

        assertDoesNotThrow(() -> libroService.eliminar("9780201633610"));

        assertFalse(libroRepository.findById("9780201633610").isPresent(),
                "Book should have been deleted");
    }
}
