package com.libromagico.service;

import com.libromagico.dto.DashboardResponse;
import com.libromagico.model.EstadoLibro;
import com.libromagico.model.EstadoMulta;
import com.libromagico.model.EstadoPrestamo;
import com.libromagico.repository.LibroRepository;
import com.libromagico.repository.MultaRepository;
import com.libromagico.repository.PrestamoRepository;
import com.libromagico.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final LibroRepository libroRepository;
    private final PrestamoRepository prestamoRepository;
    private final MultaRepository multaRepository;
    private final UsuarioRepository usuarioRepository;

    public DashboardResponse obtenerEstadisticas() {
        long totalLibros = libroRepository.count();
        long disponibles = libroRepository.countByEstado(EstadoLibro.DISPONIBLE);
        long prestados = libroRepository.countByEstado(EstadoLibro.PRESTADO);
        long perdidos = libroRepository.countByEstado(EstadoLibro.PERDIDO);
        long totalUsuarios = usuarioRepository.count();
        long prestamosActivos = prestamoRepository.countByEstado(EstadoPrestamo.ACTIVO);
        long prestamosAtrasados = prestamoRepository.countByEstadoAndFechaDevolucionBefore(
                EstadoPrestamo.ACTIVO, LocalDate.now());
        long multasPendientes = multaRepository.countByEstado(EstadoMulta.PENDIENTE);
        long multasPagadas = multaRepository.countByEstado(EstadoMulta.PAGADO);

        return new DashboardResponse(
                totalLibros, disponibles, prestados, perdidos,
                totalUsuarios, prestamosActivos, prestamosAtrasados,
                multasPendientes, multasPagadas);
    }
}
