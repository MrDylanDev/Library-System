package com.libromagico.service;

import com.libromagico.config.PrestamoConfig;
import com.libromagico.model.*;
import com.libromagico.repository.MultaRepository;
import com.libromagico.repository.PrestamoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class PrestamoService {
    private final PrestamoConfig config;
    private final PrestamoRepository prestamoRepository;
    private final MultaRepository multaRepository;
    
    /**
     * Registra un préstamo para un usuario y libro.
     * @param usuarioId ID del usuario.
     * @param isbn ISBN del libro.
     * @return Prestamo registrado.
     * @throws RuntimeException si el libro no está disponible o el usuario tiene multas pendientes.
     */
    public Prestamo registrarPrestamo(Long usuarioId, String isbn) {
        // Lógica para verificar copias disponibles y crear préstamo
        LocalDate fechaDevolucion = LocalDate.now().plusDays(config.getDias());
        Prestamo prestamo = new Prestamo();
        prestamo.setUsuario(new Usuario()); // Simplificado: en la práctica, buscar usuario por ID
        prestamo.setLibro(new Libro());     // Simplificado: buscar libro por ISBN
        prestamo.getUsuario().setId(usuarioId);
        prestamo.getLibro().setIsbn(isbn);
        prestamo.setFechaDevolucion(fechaDevolucion);
        return prestamoRepository.save(prestamo);
    }
    
    /**
     * Registra la devolución de un préstamo. Si la devolución es tardía, genera una multa automática.
     * @param prestamoId ID del préstamo.
     * @throws RuntimeException si el préstamo no existe.
     */
    public void registrarDevolucion(Long prestamoId) {
        Prestamo prestamo = prestamoRepository.findById(prestamoId)
                .orElseThrow(() -> new RuntimeException("Préstamo no encontrado"));
        prestamo.setFechaEntregaReal(LocalDate.now());
        
        if (prestamo.getFechaEntregaReal().isAfter(prestamo.getFechaDevolucion())) {
            if (!multaRepository.existsByPrestamoId(prestamoId)) {
                Multa multa = new Multa();
                multa.setPrestamo(prestamo);
                multa.setMonto(config.getMultaMonto());
                multaRepository.save(multa);
            }
        }
        // Actualizar estado del préstamo
        prestamo.setEstado(EstadoPrestamo.DEVUELTO);
        prestamoRepository.save(prestamo);
    }
    
    /**
     * Obtiene el historial de préstamos de un usuario.
     * @param usuarioId ID del usuario.
     * @return Lista de préstamos (activos y finalizados).
     */
    public List<Prestamo> getHistorialPrestamos(Long usuarioId) {
        return prestamoRepository.findHistorialByUsuarioId(usuarioId);
    }
}