package com.libromagico.repository;

import com.libromagico.model.Libro;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface LibroRepository extends JpaRepository<Libro, String> {
    List<Libro> findByAutorContainingIgnoreCase(String autor);
    List<Libro> findByTituloContainingIgnoreCase(String titulo);
    List<Libro> findByCategoriaContainingIgnoreCase(String categoria);

    @Query("SELECT l FROM Libro l WHERE " +
           "LOWER(l.titulo) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
           "LOWER(l.autor) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
           "LOWER(l.categoria) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
           "LOWER(l.isbn) LIKE LOWER(CONCAT('%', :q, '%'))")
    List<Libro> buscarGeneral(@Param("q") String q);
}
