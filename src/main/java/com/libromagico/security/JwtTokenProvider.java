package com.libromagico.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Date;
import java.util.UUID;

@Component
public class JwtTokenProvider {

    /**
     * Secret de desarrollo conocida, publicada en el historial del repositorio.
     * Solo se tolera fuera del perfil {@code prod}: en producción su uso permite
     * forjar tokens y es un fallo de arranque.
     */
    private static final String DEV_FALLBACK_SECRET =
            "LibroMagico2024SecretKeyParaFirmarJWTMinimo256Bits!!";

    private static final int MIN_SECRET_BYTES = 32;

    private final SecretKey key;
    private final long expiration;

    public JwtTokenProvider(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.expiration:86400000}") long expiration,
            Environment environment) {
        boolean prod = Arrays.asList(environment.getActiveProfiles()).contains("prod");
        if (prod) {
            requireProductionSecret(secret);
        }
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expiration = expiration;
    }

    private static void requireProductionSecret(String secret) {
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException(
                    "JWT_SECRET no está definida. En producción es obligatoria (use: openssl rand -base64 48).");
        }
        if (DEV_FALLBACK_SECRET.equals(secret)) {
            throw new IllegalStateException(
                    "JWT_SECRET es la secret por defecto de desarrollo. En producción debe ser un valor aleatorio.");
        }
        if (secret.getBytes(StandardCharsets.UTF_8).length < MIN_SECRET_BYTES) {
            throw new IllegalStateException(
                    "JWT_SECRET es demasiado corta: se requieren al menos " + MIN_SECRET_BYTES + " bytes (256 bits).");
        }
    }

    public String generateToken(String email, String rol) {
        Date now = new Date();
        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .subject(email)
                .claim("rol", rol)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + expiration))
                .signWith(key)
                .compact();
    }

    public String getTokenIdFromToken(String token) {
        return parseClaims(token).getId();
    }

    public Date getIssuedAtFromToken(String token) {
        return parseClaims(token).getIssuedAt();
    }

    public LocalDateTime getExpirationFromToken(String token) {
        return parseClaims(token).getExpiration()
                .toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
    }

    public long getExpirationMillis() {
        return expiration;
    }

    public String getEmailFromToken(String token) {
        return parseClaims(token).getSubject();
    }

    public boolean validateToken(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
