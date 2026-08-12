package com.libromagico.repository;

import com.libromagico.model.TokenRevocado;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Optional;

public interface TokenRevocadoRepository extends JpaRepository<TokenRevocado, Long> {

    boolean existsByJti(String jti);

    Optional<TokenRevocado> findByJti(String jti);

    void deleteByJti(String jti);

    void deleteByEmail(String email);

    long deleteByExpiraEnBefore(LocalDateTime now);
}
