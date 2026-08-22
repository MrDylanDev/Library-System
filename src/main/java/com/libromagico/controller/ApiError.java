package com.libromagico.controller;

import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Formato único de error de la API. Cualquier error —validación, excepción de
 * negocio, Spring Security o error no contemplado— se serializa con esta forma,
 * de modo que el cliente siempre reciba {@code {timestamp, status, error, message}}.
 */
public final class ApiError {

    private ApiError() {
    }

    public static Map<String, Object> of(HttpStatus status, String message) {
        return Map.of(
                "timestamp", LocalDateTime.now().toString(),
                "status", status.value(),
                "error", status.getReasonPhrase(),
                "message", message
        );
    }
}
