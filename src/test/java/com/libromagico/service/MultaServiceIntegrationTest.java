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
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:testtx-multa;DB_CLOSE_DELAY=-1"
})
@ActiveProfiles("test")
class MultaServiceIntegrationTest {

    @Autowired
    private MultaService multaService;

    @Autowired
    private MultaRepository multaRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private LibroRepository libroRepository;

    @Autowired
    private PrestamoRepository prestamoRepository;

    @SpyBean
    private MultaRepository spyMultaRepository;

    private Multa multaPendiente;
    private Multa multaPagada;
    private Usuario usuario;
    private Prestamo prestamo;

    @BeforeEach
    void setUp() {
        multaRepository.deleteAll();
        prestamoRepository.deleteAll();
        usuarioRepository.deleteAll();
        libroRepository.deleteAll();

        usuario = new Usuario();
        usuario.setNombre("Multa Test");
        usuario.setEmail("multa@test.com");
        usuario.setContrasena("pass123");
        usuario.setDni("22222222");
        usuario = usuarioRepository.save(usuario);

        var libro = new Libro();
        libro.setIsbn("9780451524934");
        libro.setTitulo("1984");
        libro.setAutor("George Orwell");
        libro.setCopiasDisponibles(1);
        libro.setEstado(EstadoLibro.DISPONIBLE);
        libroRepository.save(libro);

        prestamo = new Prestamo();
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
        multaPendiente = multaRepository.save(multa);

        var multaPag = new Multa();
        multaPag.setPrestamo(prestamo);
        multaPag.setMonto(new BigDecimal("10.00"));
        multaPag.setEstado(EstadoMulta.PAGADO);
        multaPagada = multaRepository.save(multaPag);
    }

    @Test
    @DisplayName("pagarMulta marks PENDIENTE multa as PAGADO")
    void pagarMulta_success() {
        var result = multaService.pagarMulta(multaPendiente.getId());

        assertNotNull(result);
        assertEquals(EstadoMulta.PAGADO, result.getEstado());
        assertEquals(multaPendiente.getId(), result.getId());
    }

    @Test
    @DisplayName("pagarMulta throws RecursoNoEncontradoException for unknown multa")
    void pagarMulta_notFound() {
        assertThrows(RecursoNoEncontradoException.class,
                () -> multaService.pagarMulta(999L));
    }

    @Test
    @DisplayName("pagarMulta throws OperacionInvalidaException for already paid multa")
    void pagarMulta_alreadyPaid() {
        assertThrows(OperacionInvalidaException.class,
                () -> multaService.pagarMulta(multaPagada.getId()));
    }

    @Test
    @DisplayName("pagarMulta rolls back on repository failure")
    void pagarMulta_rollbackOnFailure() {
        doThrow(new RuntimeException("DB failure"))
                .when(spyMultaRepository)
                .save(any(Multa.class));

        assertThrows(RuntimeException.class,
                () -> multaService.pagarMulta(multaPendiente.getId()));

        var unchanged = multaRepository.findById(multaPendiente.getId()).orElseThrow();
        assertEquals(EstadoMulta.PENDIENTE, unchanged.getEstado(),
                "Multa estado should remain PENDIENTE after rollback");
    }

    @Test
    @DisplayName("crearMulta creates a PENDIENTE multa")
    void crearMulta_success() {
        var result = multaService.crearMulta(prestamo, new BigDecimal("15.00"));

        assertNotNull(result);
        assertEquals(EstadoMulta.PENDIENTE, result.getEstado());
        assertEquals(new BigDecimal("15.00"), result.getMonto());
        assertEquals(prestamo.getId(), result.getPrestamo().getId());
    }

    @Test
    @DisplayName("listarMultas returns all multas with related data")
    void listarMultas() {
        var result = multaService.listarMultas();

        assertNotNull(result);
        assertEquals(2, result.size());
    }

    @Test
    @DisplayName("obtenerMultasPorUsuario returns only that user's multas")
    void obtenerMultasPorUsuario() {
        var result = multaService.obtenerMultasPorUsuario(usuario.getId());

        assertNotNull(result);
        assertEquals(2, result.size());
    }

    @Test
    @DisplayName("obtenerMultasPorUsuario returns empty for user without multas")
    void obtenerMultasPorUsuario_empty() {
        var result = multaService.obtenerMultasPorUsuario(999L);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }
}
