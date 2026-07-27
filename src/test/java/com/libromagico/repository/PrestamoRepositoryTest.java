package com.libromagico.repository;

import com.libromagico.model.*;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.TestPropertySource;

import jakarta.persistence.EntityManager;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@TestPropertySource(properties = {
    "spring.jpa.properties.hibernate.generate_statistics=true"
})
class PrestamoRepositoryTest {

    @Autowired
    private PrestamoRepository prestamoRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private LibroRepository libroRepository;

    @Autowired
    private EntityManager entityManager;

    private Usuario savedUser;
    private Libro savedLibro;

    @BeforeEach
    void setUp() {
        savedUser = createUser();
        savedLibro = createBook();
    }

    @Test
    @DisplayName("existsByUsuarioAndLibroAndEstado returns true when ACTIVO loan exists")
    void existsByUsuarioAndLibroAndEstado_activoLoanExists_returnsTrue() {
        createLoan(savedUser, savedLibro, EstadoPrestamo.ACTIVO);

        assertTrue(prestamoRepository.existsByUsuarioAndLibroAndEstado(
                savedUser, savedLibro, EstadoPrestamo.ACTIVO));
    }

    @Test
    @DisplayName("existsByUsuarioAndLibroAndEstado returns false when only DEVUELTO loans exist")
    void existsByUsuarioAndLibroAndEstado_onlyDevueltoLoan_returnsFalseForActivo() {
        createLoan(savedUser, savedLibro, EstadoPrestamo.DEVUELTO);

        assertFalse(prestamoRepository.existsByUsuarioAndLibroAndEstado(
                savedUser, savedLibro, EstadoPrestamo.ACTIVO));
    }

    @Test
    @DisplayName("existsByUsuarioAndLibroAndEstado returns true when both ACTIVO and DEVUELTO exist")
    void existsByUsuarioAndLibroAndEstado_bothActivoAndDevuelto_returnsTrueForActivo() {
        createLoan(savedUser, savedLibro, EstadoPrestamo.ACTIVO);
        createLoan(savedUser, savedLibro, EstadoPrestamo.DEVUELTO);

        assertTrue(prestamoRepository.existsByUsuarioAndLibroAndEstado(
                savedUser, savedLibro, EstadoPrestamo.ACTIVO));
    }

    @Test
    @DisplayName("existsByLibroAndEstado returns true when ACTIVO loan exists for libro")
    void existsByLibroAndEstado_activoLoanExists_returnsTrue() {
        createLoan(savedUser, savedLibro, EstadoPrestamo.ACTIVO);

        assertTrue(prestamoRepository.existsByLibroAndEstado(savedLibro, EstadoPrestamo.ACTIVO));
    }

    @Test
    @DisplayName("existsByLibroAndEstado returns false when no loans exist for libro")
    void existsByLibroAndEstado_noLoans_returnsFalse() {
        assertFalse(prestamoRepository.existsByLibroAndEstado(savedLibro, EstadoPrestamo.ACTIVO));
    }

    @Test
    @DisplayName("findAllWithUsuariosAndLibros returns all loans with JOIN FETCH")
    void findAllWithUsuariosAndLibros_returnsAllLoans() {
        createLoan(savedUser, savedLibro, EstadoPrestamo.ACTIVO);
        createLoan(savedUser, savedLibro, EstadoPrestamo.DEVUELTO);

        Page<Prestamo> result = prestamoRepository.findAllWithUsuariosAndLibros(PageRequest.of(0, 10));

        assertEquals(2, result.getTotalElements());
        assertNotNull(result.getContent().get(0).getUsuario().getNombre());
        assertNotNull(result.getContent().get(0).getLibro().getTitulo());
    }

    @Test
    @DisplayName("findAllWithUsuariosAndLibros executes single SQL query with JOIN FETCH — no N+1")
    void findAllWithUsuariosAndLibros_singleQuery() {
        createLoan(savedUser, savedLibro, EstadoPrestamo.ACTIVO);

        SessionFactory sf = entityManager.getEntityManagerFactory().unwrap(SessionFactory.class);
        Statistics stats = sf.getStatistics();
        stats.setStatisticsEnabled(true);
        stats.clear();

        prestamoRepository.findAllWithUsuariosAndLibros(PageRequest.of(0, 10));

        assertEquals(1, stats.getQueryExecutionCount(),
                "Should execute exactly 1 SQL query with JOIN FETCH — no N+1");
    }

    @Test
    @DisplayName("findByEstadoWithUsuariosAndLibros returns only loans matching estado")
    void findByEstadoWithUsuariosAndLibros_returnsFilteredLoans() {
        createLoan(savedUser, savedLibro, EstadoPrestamo.ACTIVO);
        createLoan(savedUser, savedLibro, EstadoPrestamo.DEVUELTO);

        Page<Prestamo> result = prestamoRepository.findByEstadoWithUsuariosAndLibros(
                EstadoPrestamo.DEVUELTO, PageRequest.of(0, 10));

        assertEquals(1, result.getTotalElements());
        assertEquals(EstadoPrestamo.DEVUELTO, result.getContent().get(0).getEstado());
    }

    @Test
    @DisplayName("findByEstadoWithUsuariosAndLibros returns empty page when no matching loans")
    void findByEstadoWithUsuariosAndLibros_noMatch_returnsEmpty() {
        createLoan(savedUser, savedLibro, EstadoPrestamo.DEVUELTO);

        Page<Prestamo> result = prestamoRepository.findByEstadoWithUsuariosAndLibros(
                EstadoPrestamo.ATRASADO, PageRequest.of(0, 10));

        assertEquals(0, result.getTotalElements());
    }

    @Test
    @DisplayName("findByEstadoWithUsuariosAndLibros executes single SQL query with JOIN FETCH")
    void findByEstadoWithUsuariosAndLibros_singleQuery() {
        createLoan(savedUser, savedLibro, EstadoPrestamo.ACTIVO);

        SessionFactory sf = entityManager.getEntityManagerFactory().unwrap(SessionFactory.class);
        Statistics stats = sf.getStatistics();
        stats.setStatisticsEnabled(true);
        stats.clear();

        prestamoRepository.findByEstadoWithUsuariosAndLibros(EstadoPrestamo.ACTIVO, PageRequest.of(0, 10));

        assertEquals(1, stats.getQueryExecutionCount(),
                "Should execute exactly 1 SQL query with JOIN FETCH");
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
