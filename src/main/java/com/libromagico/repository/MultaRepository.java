package com.libromagico.repository;

import com.libromagico.model.Multa;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface MultaRepository extends JpaRepository<Multa, Long> {

    @Query(value = "SELECT m FROM Multa m JOIN FETCH m.prestamo p JOIN FETCH p.usuario JOIN FETCH p.libro",
           countQuery = "SELECT COUNT(m) FROM Multa m")
    Page<Multa> findAllWithPrestamoAndUsuario(Pageable pageable);

    @Query("SELECT m FROM Multa m JOIN FETCH m.prestamo p JOIN FETCH p.usuario JOIN FETCH p.libro WHERE p.usuario.id = :usuarioId")
    List<Multa> findByUsuarioId(@Param("usuarioId") Long usuarioId);
}
