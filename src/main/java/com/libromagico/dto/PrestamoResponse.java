package com.libromagico.dto;

import com.libromagico.model.EstadoPrestamo;

import java.time.LocalDate;

public record PrestamoResponse(
        Long id,
        UsuarioResponse usuario,
        LibroResponse libro,
        LocalDate fechaPrestamo,
        LocalDate fechaDevolucion,
        LocalDate fechaEntregaReal,
        EstadoPrestamo estado
) {
}
