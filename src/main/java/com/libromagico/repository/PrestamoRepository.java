package com.libromagico.repository;

import com.libromagico.model.Prestamo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PrestamoRepository extends JpaRepository<Prestamo, Long> {
    // Historial de préstamos por usuario (activos + finalizados)
    @Query("SELECT p FROM Prestamo p WHERE p.usuario.id = :usuarioId ORDER BY p.fechaPrestamo DESC")
    List<Prestamo> findHistorialByUsuarioId(Long usuarioId);
    
    // Préstamos activos por usuario (para evitar multas duplicadas)
    List<Prestamo> findByUsuarioIdAndEstado(Long usuarioId, String estado);
}