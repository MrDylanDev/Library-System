package com.libromagico.dto;

/**
 * Respuesta de login/register.
 *
 * <p>El token JWT <strong>no</strong> viaja en el body: se envía únicamente en la
 * cookie httpOnly {@code AuthToken}. Aquí solo se exponen los datos de sesión
 * que el frontend necesita para el estado local.</p>
 */
public record AuthResponse(Long id, String email, String rol) {}
