package com.libromagico.service;

import com.libromagico.dto.MultaResponse;
import com.libromagico.exception.OperacionInvalidaException;
import com.libromagico.exception.RecursoNoEncontradoException;
import com.libromagico.model.EstadoMulta;
import com.libromagico.model.Multa;
import com.libromagico.model.Prestamo;
import com.libromagico.repository.MultaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class MultaService {

    private final MultaRepository multaRepository;

    @Transactional
    public Multa pagarMulta(Long multaId) {
        var multa = multaRepository.findById(multaId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Multa no encontrada: " + multaId));

        if (multa.getEstado() == EstadoMulta.PAGADO) {
            throw new OperacionInvalidaException("La multa ya fue pagada");
        }

        multa.setEstado(EstadoMulta.PAGADO);
        var saved = multaRepository.save(multa);
        log.info("Multa pagada: id={}", multaId);
        return saved;
    }

    public Page<Multa> listarMultas(Pageable pageable) {
        return multaRepository.findAllWithPrestamoAndUsuario(pageable);
    }

    public List<Multa> obtenerMultasPorUsuario(Long usuarioId) {
        return multaRepository.findByUsuarioId(usuarioId);
    }

    public Page<MultaResponse> obtenerMultasPorUsuarioPaginado(
            Long usuarioId, EstadoMulta estado, Pageable pageable) {
        return multaRepository.findByUsuarioIdAndEstado(usuarioId, estado, pageable)
                .map(this::toMultaResponse);
    }

    private MultaResponse toMultaResponse(Multa m) {
        return new MultaResponse(
                m.getId(), m.getMonto(), m.getEstado(),
                m.getPrestamo().getId(),
                m.getPrestamo().getLibro().getTitulo(),
                m.getPrestamo().getLibro().getIsbn(),
                m.getPrestamo().getFechaPrestamo(),
                m.getPrestamo().getFechaDevolucion()
        );
    }

    @Transactional
    public Multa crearMulta(Prestamo prestamo, BigDecimal monto) {
        var multa = new Multa();
        multa.setPrestamo(prestamo);
        multa.setMonto(monto);
        multa.setEstado(EstadoMulta.PENDIENTE);
        var saved = multaRepository.save(multa);
        log.info("Multa creada: prestamo={}, monto={}", prestamo.getId(), monto);
        return saved;
    }
}
