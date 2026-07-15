package com.libromagico.service;

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
    "spring.datasource.url=jdbc:h2:mem:testtx-propagation;DB_CLOSE_DELAY=-1"
})
@ActiveProfiles("test")
class TransactionPropagationTest {

    @Autowired
    private PrestamoService prestamoService;

    @Autowired
    private MultaRepository multaRepository;

    @Autowired
    private PrestamoRepository prestamoRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private LibroRepository libroRepository;

    @SpyBean
    private PrestamoRepository spyPrestamoRepository;

    private Long prestamoId;

    @BeforeEach
    void setUp() {
        multaRepository.deleteAll();
        prestamoRepository.deleteAll();
        usuarioRepository.deleteAll();
        libroRepository.deleteAll();

        var usuario = new Usuario();
        usuario.setNombre("Propagation Test");
        usuario.setEmail("propagation@test.com");
        usuario.setContrasena("pass123");
        usuario.setDni("99999991");
        usuarioRepository.save(usuario);

        var libro = new Libro();
        libro.setIsbn("9780451524935");
        libro.setTitulo("Brave New World");
        libro.setAutor("Aldous Huxley");
        libro.setCopiasDisponibles(1);
        libro.setEstado(EstadoLibro.DISPONIBLE);
        libroRepository.save(libro);

        var prestamo = new Prestamo();
        prestamo.setUsuario(usuario);
        prestamo.setLibro(libro);
        prestamo.setFechaPrestamo(LocalDate.now().minusDays(20));
        prestamo.setFechaDevolucion(LocalDate.now().minusDays(5));
        prestamo.setEstado(EstadoPrestamo.ACTIVO);
        prestamoId = prestamoRepository.save(prestamo).getId();
    }

    @Test
    @DisplayName("crearMulta joins PrestamoService tx and rolls back on failure")
    void crearMulta_rollsBackWithPrestamoService() {
        doThrow(new RuntimeException("Save failure"))
                .when(spyPrestamoRepository)
                .save(any(Prestamo.class));

        assertThrows(RuntimeException.class,
                () -> prestamoService.devolver(prestamoId));

        // Verify no multa was persisted — the crearMulta call should have been
        // rolled back when PrestamoService's transaction rolled back
        var multas = multaRepository.findAll();
        assertTrue(multas.isEmpty(),
                "Multa should be rolled back when PrestamoService transaction fails");

        // Verify the prestamo is still ACTIVE (unchanged)
        var prestamo = prestamoRepository.findById(prestamoId).orElseThrow();
        assertEquals(EstadoPrestamo.ACTIVO, prestamo.getEstado(),
                "Prestamo estado should remain ACTIVO after rollback");
        assertNull(prestamo.getFechaEntregaReal(),
                "Prestamo fechaEntregaReal should remain null after rollback");
    }
}
