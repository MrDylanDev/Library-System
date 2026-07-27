package com.libromagico.repository;

import com.libromagico.model.Libro;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

import com.libromagico.model.EstadoLibro;

public interface LibroRepository extends JpaRepository<Libro, String> {
    Page<Libro> findByAutorContainingIgnoreCase(String autor, Pageable pageable);
    Page<Libro> findByTituloContainingIgnoreCase(String titulo, Pageable pageable);
    List<Libro> findByCategoriaContainingIgnoreCase(String categoria);

    long countByEstado(EstadoLibro estado);

    @Query("SELECT l FROM Libro l WHERE " +
           "LOWER(l.titulo) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
           "LOWER(l.autor) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
           "LOWER(l.categoria) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
           "LOWER(l.isbn) LIKE LOWER(CONCAT('%', :q, '%'))")
    Page<Libro> buscarGeneral(@Param("q") String q, Pageable pageable);
}
