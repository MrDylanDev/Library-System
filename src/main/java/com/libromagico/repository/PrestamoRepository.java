package com.libromagico.repository;

import com.libromagico.model.Libro;
import com.libromagico.model.Prestamo;
import com.libromagico.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PrestamoRepository extends JpaRepository<Prestamo, Long> {
    List<Prestamo> findByUsuario(Usuario usuario);
    boolean existsByUsuarioAndLibro(Usuario usuario, Libro libro);
}
