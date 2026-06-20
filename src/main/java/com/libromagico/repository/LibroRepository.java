package com.libromagico.repository;

import com.libromagico.model.Libro;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LibroRepository extends JpaRepository<Libro, String> {
    List<Libro> findByAutorContainingIgnoreCase(String autor);
    List<Libro> findByTituloContainingIgnoreCase(String titulo);
    List<Libro> findByCategoriaContainingIgnoreCase(String categoria);
}
