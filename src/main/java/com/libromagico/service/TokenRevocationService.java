package com.libromagico.service;

import com.libromagico.model.TokenRevocado;
import com.libromagico.repository.TokenRevocadoRepository;
import com.libromagico.security.JwtTokenProvider;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.HexFormat;

@Slf4j
@Service
@RequiredArgsConstructor
public class TokenRevocationService {

    private final TokenRevocadoRepository tokenRevocadoRepository;
    private final JwtTokenProvider jwtTokenProvider;

    @Transactional
    public void revocarToken(String jti, String email, LocalDateTime expiraEn) {
        if (expiraEn.isBefore(LocalDateTime.now())) {
            // Truco de saneamiento: un token ya expirado no necesita denylist.
            return;
        }
        if (tokenRevocadoRepository.existsByJti(jti)) {
            return;
        }
        var revocado = new TokenRevocado();
        revocado.setJti(jti);
        revocado.setEmail(email);
        revocado.setExpiraEn(expiraEn);
        tokenRevocadoRepository.save(revocado);
        log.info("Token revocado: email={}, jti={}", email, jti);
    }

    public boolean estaRevocado(String jti) {
        return jti != null && tokenRevocadoRepository.existsByJti(jti);
    }

    @Transactional
    public void revocarTokensDeUsuario(String email) {
        tokenRevocadoRepository.deleteByEmail(email);

        // Marcador por usuario: la denylist por jti no puede invalidar sesiones
        // activas que nunca se cerraron sesión. Un marcador keyed por el email
        // (jti determinístico) invalida TODOS los tokens emitidos antes del
        // cambio de contraseña; los nuevos logins emiten tokens posteriores.
        var vigencia = Duration.ofMillis(jwtTokenProvider.getExpirationMillis());
        var marcador = new TokenRevocado();
        marcador.setJti(marcadorJti(email));
        marcador.setEmail(email);
        marcador.setExpiraEn(LocalDateTime.now().plus(vigencia));
        tokenRevocadoRepository.save(marcador);

        log.info("Sesiones del usuario revocadas: email={}", email);
    }

    public boolean estaTokenDeSesionPrevia(String token, String email) {
        if (token == null || email == null) {
            return false;
        }

        Date emitidoEn;
        try {
            emitidoEn = jwtTokenProvider.getIssuedAtFromToken(token);
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
        if (emitidoEn == null) {
            return false;
        }

        var marcador = tokenRevocadoRepository.findByJti(marcadorJti(email));
        if (marcador.isEmpty()) {
            return false;
        }

        var momentoCambio = marcador.get().getExpiraEn()
                .minus(Duration.ofMillis(jwtTokenProvider.getExpirationMillis()));
        var emitidoLocal = LocalDateTime.ofInstant(emitidoEn.toInstant(), ZoneId.systemDefault());
        return emitidoLocal.isBefore(momentoCambio);
    }

    @Scheduled(cron = "0 30 * * * *")
    @Transactional
    public void purgarExpirados() {
        long borrados = tokenRevocadoRepository.deleteByExpiraEnBefore(LocalDateTime.now());
        if (borrados > 0) {
            log.info("Tokens revocados expirados purgados: {}", borrados);
        }
    }

    private String marcadorJti(String email) {
        try {
            var digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(email.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 no disponible", e);
        }
    }
}