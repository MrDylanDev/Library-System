package com.libromagico.service;

import com.libromagico.model.EstadoPrestamo;
import com.libromagico.model.Multa;
import com.libromagico.repository.PrestamoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

@Slf4j
@Service
@RequiredArgsConstructor
public class PrestamoScheduler {

    private final PrestamoRepository prestamoRepository;
    private final MultaService multaService;

    @Transactional
    @Scheduled(cron = "0 0 * * * *") // cada hora
    public void marcarVencidos() {
        var vencidos = prestamoRepository.findByEstadoAndFechaDevolucionBefore(
                EstadoPrestamo.ACTIVO, LocalDate.now());

        if (vencidos.isEmpty()) {
            log.debug("No hay préstamos vencidos para procesar");
            return;
        }

        log.info("Procesando {} préstamos vencidos", vencidos.size());
        for (var prestamo : vencidos) {
            prestamo.setEstado(EstadoPrestamo.ATRASADO);
            log.info("Préstamo marcado como ATRASADO: id={}, usuario={}, libro={}",
                    prestamo.getId(), prestamo.getUsuario().getId(), prestamo.getLibro().getIsbn());
        }
        prestamoRepository.saveAll(vencidos);
    }
}
