package com.libromagico.repository;

import com.libromagico.model.Multa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MultaRepository extends JpaRepository<Multa, Long> {
    // Verificar si un préstamo ya tiene multa asociada
    boolean existsByPrestamoId(Long prestamoId);
}