package com.libromagico.service;

import com.libromagico.exception.OperacionInvalidaException;
import com.libromagico.exception.RecursoNoEncontradoException;
import com.libromagico.model.*;
import com.libromagico.repository.LibroRepository;
import com.libromagico.repository.MultaRepository;
import com.libromagico.repository.PrestamoRepository;
import com.libromagico.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:testdevolver-libromagico;DB_CLOSE_DELAY=-1"
})
@ActiveProfiles("test")
class PrestamoDevolverTest {

    @Autowired
    private PrestamoService prestamoService;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private LibroRepository libroRepository;

    @Autowired
    private PrestamoRepository prestamoRepository;

    @Autowired
    private MultaRepository multaRepository;

    @MockBean
    private EmailService emailService;

    private Usuario usuario;
    private Libro libro;

    @BeforeEach
    void setUp() {
        multaRepository.deleteAll();
        prestamoRepository.deleteAll();
        usuarioRepository.deleteAll();
        libroRepository.deleteAll();

        reset(emailService);

        usuario = new Usuario();
        usuario.setNombre("Test User");
        usuario.setEmail("testuser@test.com");
        usuario.setContrasena("pass123");
        usuario.setDni("12345678");
        usuario.setTelefono("+5491111111111");
        usuarioRepository.save(usuario);

        libro = new Libro();
        libro.setIsbn("9780201633610");
        libro.setTitulo("Design Patterns");
        libro.setAutor("Gang of Four");
        libro.setCopiasDisponibles(3);
        libro.setEstado(EstadoLibro.DISPONIBLE);
        libroRepository.save(libro);
    }

    @Test
    @DisplayName("devolver() a tiempo — estado DEVUELTO, fechaEntregaReal set, sin multa")
    void devolver_aTiempo_exito() {
        var prestamo = prestamoService.prestar(usuario.getId(), libro.getIsbn());
        assertNotNull(prestamo.getId());
        assertEquals(EstadoPrestamo.ACTIVO, prestamo.getEstado());

        var devuelto = prestamoService.devolver(prestamo.getId());

        assertEquals(EstadoPrestamo.DEVUELTO, devuelto.getEstado());
        assertNotNull(devuelto.getFechaEntregaReal());
        assertEquals(LocalDate.now(), devuelto.getFechaEntregaReal());

        var multas = multaRepository.findAll();
        var multaDelPrestamo = multas.stream()
                .filter(m -> m.getPrestamo().getId().equals(prestamo.getId()))
                .findFirst();
        assertFalse(multaDelPrestamo.isPresent(), "No debería haber multa para devolución a tiempo");
    }

    @Test
    @DisplayName("devolver() tardía genera multa con monto correcto")
    void devolver_tardia_generaMulta() {
        var prestamo = new Prestamo();
        prestamo.setUsuario(usuario);
        prestamo.setLibro(libro);
        prestamo.setFechaPrestamo(LocalDate.now().minusDays(20));
        prestamo.setFechaDevolucion(LocalDate.now().minusDays(5));
        prestamo.setEstado(EstadoPrestamo.ACTIVO);
        var saved = prestamoRepository.save(prestamo);

        var devuelto = prestamoService.devolver(saved.getId());

        assertEquals(EstadoPrestamo.DEVUELTO, devuelto.getEstado());

        var multas = multaRepository.findAll();
        var multa = multas.stream()
                .filter(m -> m.getPrestamo().getId().equals(saved.getId()))
                .findFirst();
        assertTrue(multa.isPresent(), "Debería existir una multa para devolución tardía");
        assertEquals(new BigDecimal("10.00"), multa.get().getMonto());
        assertEquals(EstadoMulta.PENDIENTE, multa.get().getEstado());
    }

    @Test
    @DisplayName("devolver() tardía envía email de notificación")
    void devolver_tardia_enviaEmail() {
        var prestamo = new Prestamo();
        prestamo.setUsuario(usuario);
        prestamo.setLibro(libro);
        prestamo.setFechaPrestamo(LocalDate.now().minusDays(20));
        prestamo.setFechaDevolucion(LocalDate.now().minusDays(5));
        prestamo.setEstado(EstadoPrestamo.ACTIVO);
        var saved = prestamoRepository.save(prestamo);

        prestamoService.devolver(saved.getId());

        verify(emailService).notificarDevolucionTardia(
                usuario.getEmail(), libro.getTitulo(), new BigDecimal("10.00"));
    }

    @Test
    @DisplayName("devolver() préstamo ya devuelto lanza OperacionInvalidaException")
    void devolver_yaDevuelto_lanzaExcepcion() {
        var prestamo = prestamoService.prestar(usuario.getId(), libro.getIsbn());
        prestamoService.devolver(prestamo.getId());

        var ex = assertThrows(OperacionInvalidaException.class,
                () -> prestamoService.devolver(prestamo.getId()));
        assertEquals("El préstamo ya fue devuelto", ex.getMessage());
    }

    @Test
    @DisplayName("devolver() préstamo inexistente lanza RecursoNoEncontradoException")
    void devolver_inexistente_lanzaExcepcion() {
        var ex = assertThrows(RecursoNoEncontradoException.class,
                () -> prestamoService.devolver(99999L));
        assertTrue(ex.getMessage().contains("Préstamo no encontrado"));
    }
}
