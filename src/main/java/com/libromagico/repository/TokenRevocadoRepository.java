package com.libromagico.repository;

import com.libromagico.model.TokenRevocado;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface TokenRevocadoRepository extends JpaRepository<TokenRevocado, Long> {

    boolean existsByJti(String jti);

    Optional<TokenRevocado> findByJti(String jti);

    void deleteByJti(String jti);

    @Modifying
    @Query("delete from TokenRevocado t where t.email = :email")
    void deleteByEmail(@Param("email") String email);

    long deleteByExpiraEnBefore(LocalDateTime now);
}
