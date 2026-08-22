package com.libromagico.dto;

import com.libromagico.model.EstadoMulta;

import java.math.BigDecimal;

public record MultaAdminResponse(
        Long id,
        BigDecimal monto,
        EstadoMulta estado,
        PrestamoResponse prestamo
) {
}
