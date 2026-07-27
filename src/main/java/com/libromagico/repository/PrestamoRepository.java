package com.libromagico.repository;

import com.libromagico.model.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

import com.libromagico.model.EstadoPrestamo;

public interface PrestamoRepository extends JpaRepository<Prestamo, Long> {
    Page<Prestamo> findByUsuario(Usuario usuario, Pageable pageable);

    boolean existsByUsuarioAndLibroAndEstado(
            @Param("usuario") Usuario usuario,
            @Param("libro") Libro libro,
            @Param("estado") EstadoPrestamo estado);

    boolean existsByLibroAndEstado(Libro libro, EstadoPrestamo estado);
    long countByEstado(EstadoPrestamo estado);
    long countByEstadoAndFechaDevolucionBefore(EstadoPrestamo estado, LocalDate fecha);

    @Query("SELECT p FROM Prestamo p JOIN FETCH p.usuario JOIN FETCH p.libro WHERE p.estado = :estado AND p.fechaDevolucion < :fecha")
    List<Prestamo> findByEstadoAndFechaDevolucionBefore(@Param("estado") EstadoPrestamo estado, @Param("fecha") LocalDate fecha);

    @Query(value = "SELECT p FROM Prestamo p JOIN FETCH p.usuario JOIN FETCH p.libro",
           countQuery = "SELECT COUNT(p) FROM Prestamo p")
    Page<Prestamo> findAllWithUsuariosAndLibros(Pageable pageable);

    @Query(value = "SELECT p FROM Prestamo p JOIN FETCH p.usuario JOIN FETCH p.libro WHERE p.estado = :estado",
           countQuery = "SELECT COUNT(p) FROM Prestamo p WHERE p.estado = :estado")
    Page<Prestamo> findByEstadoWithUsuariosAndLibros(@Param("estado") EstadoPrestamo estado, Pageable pageable);
}
