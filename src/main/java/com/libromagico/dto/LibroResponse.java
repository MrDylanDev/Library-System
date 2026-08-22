package com.libromagico.dto;

import com.libromagico.model.EstadoLibro;

public record LibroResponse(
        String isbn,
        String titulo,
        String autor,
        String categoria,
        Integer añoPub,
        String editorial,
        Integer copiasDisponibles,
        EstadoLibro estado
) {
}
