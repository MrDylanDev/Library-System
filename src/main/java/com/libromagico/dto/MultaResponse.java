package com.libromagico.dto;

import com.libromagico.model.EstadoMulta;

import java.math.BigDecimal;
import java.time.LocalDate;

public record MultaResponse(
    Long id,
    BigDecimal monto,
    EstadoMulta estado,
    Long prestamoId,
    String libroTitulo,
    String libroIsbn,
    LocalDate fechaPrestamo,
    LocalDate fechaDevolucion
) {}
