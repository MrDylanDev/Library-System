package com.libromagico.repository;

import com.libromagico.model.Libro;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LibroRepository extends JpaRepository<Libro, String> {
    // Búsqueda por título, autor o categoría (case-insensitive)
    List<Libro> findByTituloContainingIgnoreCase(String titulo);
    List<Libro> findByAutorContainingIgnoreCase(String autor);
    List<Libro> findByCategoriaContainingIgnoreCase(String categoria);
    
    // Verificar disponibilidad
    boolean existsByIsbnAndCopiasDisponiblesGreaterThan(String isbn, int copias);
}