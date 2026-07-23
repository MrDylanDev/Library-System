package com.libromagico.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import com.libromagico.validation.NotFutureYear;
import lombok.*;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "libros")
public class Libro {
    @Id
    @NotBlank(message = "El ISBN es obligatorio")
    @Pattern(regexp = "^(?:\\d{9}[\\dXx]|\\d{13})$", message = "ISBN inválido")
    private String isbn;

    @NotBlank(message = "El título es obligatorio")
    private String titulo;

    @NotBlank(message = "El autor es obligatorio")
    private String autor;

    private String categoria;

    @Min(value = 1900, message = "Año de publicación inválido")
    @NotFutureYear
    private Integer añoPub;

    private String editorial;

    private Integer copiasDisponibles = 1;

    @Enumerated(EnumType.STRING)
    private EstadoLibro estado = EstadoLibro.DISPONIBLE;
}