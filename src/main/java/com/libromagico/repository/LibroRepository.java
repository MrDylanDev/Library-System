package com.libromagico.repository;

import com.libromagico.model.Libro;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

import java.util.List;
import java.util.Optional;

import com.libromagico.model.EstadoLibro;

public interface LibroRepository extends JpaRepository<Libro, String> {
    Page<Libro> findByAutorContainingIgnoreCase(String autor, Pageable pageable);
    Page<Libro> findByTituloContainingIgnoreCase(String titulo, Pageable pageable);
    List<Libro> findByCategoriaContainingIgnoreCase(String categoria);

    long countByEstado(EstadoLibro estado);

    /**
     * Lee la fila del libro con bloqueo pesimista de escritura. Se usa en el
     * alta de un préstamo para serializar el decremento de {@code copiasDisponibles}
     * y evitar que dos peticiones concurrentes presten la última copia.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT l FROM Libro l WHERE l.isbn = :isbn")
    Optional<Libro> findByIdForUpdate(@Param("isbn") String isbn);

    @Query("SELECT l FROM Libro l WHERE " +
           "LOWER(l.titulo) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
           "LOWER(l.autor) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
           "LOWER(l.categoria) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
           "LOWER(l.isbn) LIKE LOWER(CONCAT('%', :q, '%'))")
    Page<Libro> buscarGeneral(@Param("q") String q, Pageable pageable);
}

