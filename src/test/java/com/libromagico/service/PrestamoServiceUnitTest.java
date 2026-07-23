package com.libromagico.service;

import com.libromagico.config.PrestamoConfig;
import com.libromagico.exception.OperacionInvalidaException;
import com.libromagico.model.*;
import com.libromagico.repository.PrestamoRepository;
import com.libromagico.repository.UsuarioRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PrestamoServiceUnitTest {

    @Mock
    private PrestamoRepository prestamoRepository;

    @Mock
    private LibroService libroService;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private MultaService multaService;

    @Mock
    private PrestamoConfig prestamoConfig;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private PrestamoService prestamoService;

    @Test
    @DisplayName("prestar() libro no disponible lanza OperacionInvalidaException")
    void prestar_libroNoDisponible_lanzaExcepcion() {
        var usuario = new Usuario();
        usuario.setId(1L);
        var libro = new Libro();
        libro.setIsbn("9780201633610");
        libro.setEstado(EstadoLibro.PRESTADO);
        libro.setCopiasDisponibles(0);

        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        when(libroService.buscarPorIsbn("9780201633610")).thenReturn(libro);

        assertThrows(OperacionInvalidaException.class,
                () -> prestamoService.prestar(1L, "9780201633610"));
    }

    @Test
    @DisplayName("prestar() usuario ya tiene préstamo activo lanza OperacionInvalidaException")
    void prestar_usuarioYaTienePrestamoActivo_lanzaExcepcion() {
        var usuario = new Usuario();
        usuario.setId(1L);
        var libro = new Libro();
        libro.setIsbn("9780201633610");
        libro.setEstado(EstadoLibro.DISPONIBLE);
        libro.setCopiasDisponibles(2);

        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        when(libroService.buscarPorIsbn("9780201633610")).thenReturn(libro);
        when(prestamoRepository.existsByUsuarioAndLibroAndEstado(any(), any(), eq(EstadoPrestamo.ACTIVO)))
                .thenReturn(true);

        assertThrows(OperacionInvalidaException.class,
                () -> prestamoService.prestar(1L, "9780201633610"));
    }

    @Test
    @DisplayName("devolver() préstamo ACTIVO a tiempo — éxito, sin multa ni email")
    void devolver_aTiempo_exito() {
        var usuario = new Usuario();
        usuario.setEmail("user@test.com");
        var libro = new Libro();
        libro.setTitulo("Test Book");
        libro.setCopiasDisponibles(1);
        var prestamo = new Prestamo();
        prestamo.setId(1L);
        prestamo.setUsuario(usuario);
        prestamo.setLibro(libro);
        prestamo.setEstado(EstadoPrestamo.ACTIVO);
        prestamo.setFechaDevolucion(LocalDate.now().plusDays(5));
        prestamo.setFechaPrestamo(LocalDate.now());

        when(prestamoRepository.findById(1L)).thenReturn(Optional.of(prestamo));
        doNothing().when(libroService).actualizarDisponibilidad(any(), any(), anyInt());
        when(prestamoRepository.save(any(Prestamo.class))).thenAnswer(i -> i.getArgument(0));

        var result = prestamoService.devolver(1L);

        assertEquals(EstadoPrestamo.DEVUELTO, result.getEstado());
        assertNotNull(result.getFechaEntregaReal());
        assertEquals(LocalDate.now(), result.getFechaEntregaReal());
        verify(prestamoRepository).save(prestamo);
        verify(multaService, never()).crearMulta(any(Prestamo.class), any(BigDecimal.class));
        verify(emailService, never()).notificarDevolucionTardia(anyString(), anyString(), any(BigDecimal.class));
    }

    @Test
    @DisplayName("devolver() préstamo ACTIVO y vencido — genera multa y email")
    void devolver_vencido_generaMultaYEmail() {
        var usuario = new Usuario();
        usuario.setEmail("user@test.com");
        var libro = new Libro();
        libro.setTitulo("Test Book");
        libro.setCopiasDisponibles(1);
        var prestamo = new Prestamo();
        prestamo.setId(1L);
        prestamo.setUsuario(usuario);
        prestamo.setLibro(libro);
        prestamo.setEstado(EstadoPrestamo.ACTIVO);
        prestamo.setFechaDevolucion(LocalDate.now().minusDays(5));
        prestamo.setFechaPrestamo(LocalDate.now().minusDays(20));

        when(prestamoRepository.findById(1L)).thenReturn(Optional.of(prestamo));
        when(prestamoConfig.getMultaMonto()).thenReturn(new BigDecimal("10.00"));
        doNothing().when(libroService).actualizarDisponibilidad(any(), any(), anyInt());
        when(prestamoRepository.save(any(Prestamo.class))).thenAnswer(i -> i.getArgument(0));

        prestamoService.devolver(1L);

        assertEquals(EstadoPrestamo.DEVUELTO, prestamo.getEstado());
        assertNotNull(prestamo.getFechaEntregaReal());
        assertEquals(LocalDate.now(), prestamo.getFechaEntregaReal());
        verify(prestamoRepository).save(prestamo);
        verify(multaService).crearMulta(prestamo, new BigDecimal("10.00"));
        verify(emailService).notificarDevolucionTardia(
                usuario.getEmail(), libro.getTitulo(), new BigDecimal("10.00"));
    }

    @Test
    @DisplayName("devolver() préstamo no ACTIVO lanza OperacionInvalidaException")
    void devolver_prestamoNoActivo_lanzaExcepcion() {
        var prestamo = new Prestamo();
        prestamo.setId(1L);
        prestamo.setEstado(EstadoPrestamo.DEVUELTO);

        when(prestamoRepository.findById(1L)).thenReturn(Optional.of(prestamo));

        var ex = assertThrows(OperacionInvalidaException.class,
                () -> prestamoService.devolver(1L));
        assertEquals("El préstamo ya fue devuelto", ex.getMessage());
    }
}
