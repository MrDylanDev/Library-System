package com.libromagico.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.libromagico.controller.ApiError;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletResponseWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Set;

/**
 * Filtro de rate limiting para los endpoints de autenticación.
 *
 * <p>Limita por IP todos los POST a {@code /api/auth/login},
 * {@code /api/auth/register} y {@code /api/auth/forgot-password}. El límite
 * por email se aplica solo a {@code /api/auth/login} y únicamente ante fallos
 * reales de autenticación (respuesta 401), con una clave compuesta por email e
 * IP para que un atacante no pueda bloquear la cuenta de una víctima desde
 * cualquier origen. Responde {@code 429 Too Many Requests} con JSON consistente
 * con el resto de la API cuando se supera un límite.</p>
 *
 * <p>La IP se toma de {@code remoteAddr}; el header {@code X-Forwarded-For}
 * solo se usa cuando el peer figura en {@code auth.rate-limit.trusted-proxies}
 * (por defecto vacío, es decir, el header nunca se confía).</p>
 */
@Component
@RequiredArgsConstructor
public class AuthRateLimitFilter extends OncePerRequestFilter {

    private static final Set<String> PROTECTED_PATHS = Set.of(
            "/api/auth/login",
            "/api/auth/register",
            "/api/auth/forgot-password");

    private static final String IP_KEY_PREFIX = "ip:";
    private static final String EMAIL_KEY_PREFIX = "email:";

    private static final String MESSAGE_TOO_MANY_REQUESTS =
            "Demasiados intentos. Intente nuevamente más tarde.";

    private final AuthRateLimiter rateLimiter;
    private final ObjectMapper objectMapper;

    @Value("${auth.rate-limit.enabled:true}")
    private boolean enabled;

    @Value("${auth.rate-limit.ip.max-attempts:60}")
    private int ipMaxAttempts;

    @Value("${auth.rate-limit.ip.window-minutes:1}")
    private long ipWindowMinutes;

    @Value("${auth.rate-limit.email.max-attempts:5}")
    private int emailMaxAttempts;

    @Value("${auth.rate-limit.email.window-minutes:15}")
    private long emailWindowMinutes;

    @Value("${auth.rate-limit.trusted-proxies:}")
    private List<String> trustedProxies;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !enabled
                || !HttpMethod.POST.matches(request.getMethod())
                || !PROTECTED_PATHS.contains(request.getRequestURI());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String ip = clientIp(request);
        String ipKey = IP_KEY_PREFIX + ip;
        if (!rateLimiter.tryAcquire(ipKey, ipMaxAttempts, Duration.ofMinutes(ipWindowMinutes))) {
            writeTooManyRequests(response);
            return;
        }

        if ("/api/auth/login".equals(request.getRequestURI())) {
            handleLogin(request, response, filterChain, ipKey, ip);
            return;
        }

        filterChain.doFilter(request, response);
    }

    private void handleLogin(HttpServletRequest request,
                             HttpServletResponse response,
                             FilterChain filterChain,
                             String ipKey,
                             String ip) throws ServletException, IOException {
        CachedBodyRequestWrapper cachedRequest = new CachedBodyRequestWrapper(request);
        String email = extractEmail(cachedRequest);
        String emailKey = (email == null) ? null : EMAIL_KEY_PREFIX + email + "|" + ipKey;

        if (emailKey != null
                && rateLimiter.isBlocked(emailKey, emailMaxAttempts, Duration.ofMinutes(emailWindowMinutes))) {
            writeTooManyRequests(response);
            return;
        }

        StatusCapturingResponseWrapper wrappedResponse = new StatusCapturingResponseWrapper(response);
        filterChain.doFilter(cachedRequest, wrappedResponse);

        if (emailKey != null && wrappedResponse.getStatus() == HttpServletResponse.SC_UNAUTHORIZED) {
            rateLimiter.tryAcquire(emailKey, emailMaxAttempts, Duration.ofMinutes(emailWindowMinutes));
        }
    }

    private String clientIp(HttpServletRequest request) {
        String remoteAddr = request.getRemoteAddr();
        String peer = (remoteAddr == null || remoteAddr.isBlank()) ? "unknown" : remoteAddr;
        if (trustedProxies.contains(peer)) {
            String forwarded = request.getHeader("X-Forwarded-For");
            if (forwarded != null && !forwarded.isBlank()) {
                String[] hops = forwarded.split(",");
                for (int i = hops.length - 1; i >= 0; i--) {
                    String hop = hops[i].trim();
                    if (!hop.isBlank()) {
                        return hop;
                    }
                }
            }
        }
        return peer;
    }

    private String extractEmail(HttpServletRequest request) {
        try {
            JsonNode root = objectMapper.readTree(request.getInputStream());
            JsonNode emailNode = root.get("email");
            if (emailNode == null || emailNode.isNull()) {
                return null;
            }
            String email = emailNode.asText();
            return email.isBlank() ? null : email;
        } catch (IOException e) {
            return null;
        }
    }

    private void writeTooManyRequests(HttpServletResponse response) throws IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setStatus(429);
        response.getWriter().write(objectMapper.writeValueAsString(
            ApiError.of(HttpStatus.TOO_MANY_REQUESTS, MESSAGE_TOO_MANY_REQUESTS)));
    }

    /**
     * Captura el status final de la respuesta para que el filtro pueda detectar
     * si la autenticación falló (401) sin cortar el flujo de la cadena.
     */
    private static final class StatusCapturingResponseWrapper extends HttpServletResponseWrapper {

        private int statusCode = HttpServletResponse.SC_OK;

        StatusCapturingResponseWrapper(HttpServletResponse response) {
            super(response);
        }

        @Override
        public int getStatus() {
            return statusCode;
        }

        @Override
        public void setStatus(int sc) {
            statusCode = sc;
            super.setStatus(sc);
        }

        @Override
        public void sendError(int sc) throws IOException {
            statusCode = sc;
            super.sendError(sc);
        }

        @Override
        public void sendError(int sc, String msg) throws IOException {
            statusCode = sc;
            super.sendError(sc, msg);
        }
    }

    /**
     * Envuelve la petición cacheando el body para que el controller pueda
     * releerlo. {@code ContentCachingRequestWrapper} solo permite recuperar el
     * body cacheado vía {@code getContentAsByteArray()}; Spring MVC deserializa
     * el {@code @RequestBody} desde el {@code ServletInputStream}, por lo que
     * aquí se re-expone el body cacheado como un stream re-lectable.
     */
    private static final class CachedBodyRequestWrapper extends HttpServletRequestWrapper {

        private final byte[] body;

        CachedBodyRequestWrapper(HttpServletRequest request) throws IOException {
            super(request);
            this.body = request.getInputStream().readAllBytes();
        }

        @Override
        public ServletInputStream getInputStream() {
            ByteArrayInputStream in = new ByteArrayInputStream(body);
            return new ServletInputStream() {
                @Override
                public boolean isFinished() {
                    return in.available() == 0;
                }

                @Override
                public boolean isReady() {
                    return true;
                }

                @Override
                public void setReadListener(ReadListener readListener) {
                    // No-op: stream síncrono.
                }

                @Override
                public int read() {
                    return in.read();
                }
            };
        }

        @Override
        public BufferedReader getReader() {
            return new BufferedReader(new InputStreamReader(getInputStream(), StandardCharsets.UTF_8));
        }

        @Override
        public int getContentLength() {
            return body.length;
        }

        @Override
        public long getContentLengthLong() {
            return body.length;
        }
    }
}