package com.libromagico.repository;

import com.libromagico.model.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PrestamoRepository extends JpaRepository<Prestamo, Long> {
    List<Prestamo> findByUsuario(Usuario usuario);

    boolean existsByUsuarioAndLibroAndEstado(
            @Param("usuario") Usuario usuario,
            @Param("libro") Libro libro,
            @Param("estado") EstadoPrestamo estado);

    boolean existsByLibroAndEstado(Libro libro, EstadoPrestamo estado);
}
