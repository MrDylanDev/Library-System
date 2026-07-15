package com.libromagico.service;

import com.libromagico.config.PrestamoConfig;
import com.libromagico.exception.OperacionInvalidaException;
import com.libromagico.exception.RecursoNoEncontradoException;
import com.libromagico.model.*;
import com.libromagico.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class PrestamoService {

    private final PrestamoRepository prestamoRepository;
    private final LibroService libroService;
    private final UsuarioRepository usuarioRepository;
    private final MultaService multaService;
    private final PrestamoConfig prestamoConfig;
    private final EmailService emailService;

    public Prestamo prestar(Long usuarioId, String libroIsbn) {
        var usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado: " + usuarioId));
        var libro = libroService.buscarPorIsbn(libroIsbn);

        if (prestamoRepository.existsByUsuarioAndLibroAndEstado(usuario, libro, EstadoPrestamo.ACTIVO)) {
            throw new OperacionInvalidaException("El usuario ya tiene un préstamo activo de este libro");
        }

        if (libro.getEstado() != EstadoLibro.DISPONIBLE || libro.getCopiasDisponibles() <= 0) {
            throw new OperacionInvalidaException("El libro no está disponible para préstamo");
        }

        var prestamo = new Prestamo();
        prestamo.setUsuario(usuario);
        prestamo.setLibro(libro);
        prestamo.setFechaPrestamo(LocalDate.now());
        prestamo.setFechaDevolucion(LocalDate.now().plusDays(prestamoConfig.getDias()));
        prestamo.setEstado(EstadoPrestamo.ACTIVO);

        libroService.actualizarDisponibilidad(libro,
                libro.getCopiasDisponibles() == 1 ? EstadoLibro.PRESTADO : EstadoLibro.DISPONIBLE,
                -1);

        var saved = prestamoRepository.save(prestamo);
        log.info("Préstamo creado: usuario={}, libro={}, devolución={}", usuarioId, libroIsbn, saved.getFechaDevolucion());
        return saved;
    }

    public Prestamo devolver(Long prestamoId) {
        var prestamo = prestamoRepository.findById(prestamoId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Préstamo no encontrado: " + prestamoId));

        if (prestamo.getEstado() != EstadoPrestamo.ACTIVO) {
            throw new OperacionInvalidaException("El préstamo ya fue devuelto");
        }

        prestamo.setFechaEntregaReal(LocalDate.now());
        prestamo.setEstado(EstadoPrestamo.DEVUELTO);

        var libro = prestamo.getLibro();
        libroService.actualizarDisponibilidad(libro,
                libro.getCopiasDisponibles() == 0 ? EstadoLibro.DISPONIBLE : libro.getEstado(),
                1);

        if (prestamo.getFechaEntregaReal().isAfter(prestamo.getFechaDevolucion())) {
            BigDecimal monto = prestamoConfig.getMultaMonto();
            multaService.crearMulta(prestamo, monto);

            var usuario = prestamo.getUsuario();
            log.info("Multa generada: usuario={}, monto={}", usuario.getId(), monto);
            emailService.notificarDevolucionTardia(usuario.getEmail(),
                    prestamo.getLibro().getTitulo(),
                    prestamoConfig.getMultaMonto());
        }

        var saved = prestamoRepository.save(prestamo);
        log.info("Devolución registrada: préstamo={}, fechaReal={}", prestamoId, prestamo.getFechaEntregaReal());
        return saved;
    }

    public List<Prestamo> historialPorUsuario(Long usuarioId) {
        var usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado: " + usuarioId));
        return prestamoRepository.findByUsuario(usuario);
    }

    public List<Prestamo> listarTodos() {
        return prestamoRepository.findAll();
    }
}
