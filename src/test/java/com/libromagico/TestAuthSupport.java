package com.libromagico;

import com.libromagico.security.JwtAuthenticationFilter;
import jakarta.servlet.http.Cookie;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Arrays;

/**
 * Utilidades compartidas para tests de integración.
 *
 * <p>El JWT ya no viaja en el body de login/register: se envía en la cookie
 * httpOnly {@code AuthToken}. Estos helpers extraen el token de la cookie
 * de la respuesta para autenticar las siguientes peticiones del test.</p>
 */
public final class TestAuthSupport {

    public static final String AUTH_COOKIE = JwtAuthenticationFilter.AUTH_COOKIE;

    private TestAuthSupport() {
    }

    public static String extractToken(MvcResult result) {
        return Arrays.stream(result.getResponse().getCookies())
                .filter(c -> AUTH_COOKIE.equals(c.getName()))
                .map(Cookie::getValue)
                .findFirst()
                .orElseThrow(() -> new AssertionError(
                        "No se encontró la cookie " + AUTH_COOKIE + " en la respuesta"));
    }
}
