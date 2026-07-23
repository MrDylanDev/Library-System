package com.libromagico.repository;

import com.libromagico.model.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface PrestamoRepository extends JpaRepository<Prestamo, Long> {
    List<Prestamo> findByUsuario(Usuario usuario);

    boolean existsByUsuarioAndLibroAndEstado(
            @Param("usuario") Usuario usuario,
            @Param("libro") Libro libro,
            @Param("estado") EstadoPrestamo estado);

    boolean existsByLibroAndEstado(Libro libro, EstadoPrestamo estado);

    @Query("SELECT p FROM Prestamo p JOIN FETCH p.usuario JOIN FETCH p.libro WHERE p.estado = :estado AND p.fechaDevolucion < :fecha")
    List<Prestamo> findByEstadoAndFechaDevolucionBefore(@Param("estado") EstadoPrestamo estado, @Param("fecha") LocalDate fecha);
}
