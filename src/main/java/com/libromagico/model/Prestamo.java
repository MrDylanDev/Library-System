package com.libromagico.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "prestamos")
public class Prestamo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    @ManyToOne
    @JoinColumn(name = "libro_isbn", referencedColumnName = "isbn")
    private Libro libro;

    private LocalDate fechaPrestamo = LocalDate.now();
    private LocalDate fechaDevolucion;
    private LocalDate fechaEntregaReal;

    @Enumerated(EnumType.STRING)
    private EstadoPrestamo estado = EstadoPrestamo.ACTIVO;
}

public enum EstadoPrestamo {
    ACTIVO, DEVUELTO, ATRASADO
}