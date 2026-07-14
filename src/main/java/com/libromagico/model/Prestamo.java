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
@NamedQuery(
    name = "Prestamo.existsByUsuarioAndLibroAndEstado",
    query = "SELECT CASE WHEN COUNT(p) > 0 THEN true ELSE false END " +
            "FROM Prestamo p WHERE p.usuario = :usuario " +
            "AND p.libro = :libro AND p.estado = :estado"
)
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