package com.libromagico.security;

import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Limitador de intentos en memoria con ventana deslizante de timestamps.
 *
 * <p>Mantiene por clave ({@code ip:...} o {@code email:...}) una cola de
 * timestamps (epoch millis). {@link #tryAcquire(String, int, Duration)}
 * descarta los intentos fuera de la ventana y rechaza cuando la cola alcanza
 * el máximo de intentos. La limpieza es doble: perezosa (se descartan
 * entradas vencidas al acceder a la clave) y periódica (una tarea {@link #purge()}
 * cada 60 segundos) para que las claves inactivas no queden acumuladas en
 * memoria.</p>
 *
 * <p>El purgado periódico usa como umbral la ventana más larga observada por
 * {@link #tryAcquire(String, int, Duration)} o {@link #isBlocked(String, int, Duration)},
 * de modo que nunca descarta entradas aún dentro de su ventana y toda clave
 * termina siendo removida poco después de expirar.</p>
 */
@Component
public class AuthRateLimiter implements AutoCloseable {

    private static final long PURGE_INTERVAL_SECONDS = 60;

    private final Clock clock;
    private final ConcurrentHashMap<String, Deque<Long>> attempts = new ConcurrentHashMap<>();
    private final ScheduledExecutorService purgeExecutor;
    private final AtomicLong maxWindowMillis = new AtomicLong();

    public AuthRateLimiter(Clock clock) {
        this.clock = clock;
        this.purgeExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r);
            t.setDaemon(true);
            return t;
        });
        this.purgeExecutor.scheduleWithFixedDelay(
                this::purge, PURGE_INTERVAL_SECONDS, PURGE_INTERVAL_SECONDS, TimeUnit.SECONDS);
    }

    /**
     * Registra un intento para {@code key} si la ventana deslizante lo permite.
     *
     * @param key         identificador de la ventana (ej. {@code ip:127.0.0.1})
     * @param maxAttempts cantidad máxima de intentos dentro de la ventana
     * @param window      duración de la ventana deslizante
     * @return {@code true} si el intento está permitido; {@code false} si se superó el límite
     */
    public boolean tryAcquire(String key, int maxAttempts, Duration window) {
        trackWindow(window);
        long now = clock.millis();
        long cutoff = now - window.toMillis();

        AtomicBoolean allowed = new AtomicBoolean(false);
        attempts.compute(key, (k, deque) -> {
            if (deque == null) {
                deque = new ArrayDeque<>();
            }
            while (!deque.isEmpty() && deque.peekFirst() < cutoff) {
                deque.pollFirst();
            }
            if (deque.size() >= maxAttempts) {
                return deque.isEmpty() ? null : deque;
            }
            deque.addLast(now);
            allowed.set(true);
            return deque;
        });
        return allowed.get();
    }

    /**
     * Consulta si {@code key} ya alcanzó el máximo de intentos dentro de su
     * ventana, SIN registrar un nuevo intento. Realiza la misma limpieza
     * perezosa de entradas vencidas que {@link #tryAcquire(String, int, Duration)}.
     *
     * @param key         identificador de la ventana
     * @param maxAttempts cantidad máxima de intentos dentro de la ventana
     * @param window      duración de la ventana deslizante
     * @return {@code true} si la cola (tras limpiar vencidos) tiene {@code >= maxAttempts} entradas
     */
    public boolean isBlocked(String key, int maxAttempts, Duration window) {
        trackWindow(window);
        long now = clock.millis();
        long cutoff = now - window.toMillis();

        AtomicBoolean blocked = new AtomicBoolean(false);
        attempts.computeIfPresent(key, (k, deque) -> {
            while (!deque.isEmpty() && deque.peekFirst() < cutoff) {
                deque.pollFirst();
            }
            if (deque.isEmpty()) {
                return null;
            }
            if (deque.size() >= maxAttempts) {
                blocked.set(true);
            }
            return deque;
        });
        return blocked.get();
    }

    /**
     * Evita el crecimiento no acotado del mapa: recorre las claves, descarta
     * timestamps vencidos y elimina las que quedaron sin entradas. Se ejecuta
     * en un hilo daemon cada {@value #PURGE_INTERVAL_SECONDS} segundos.
     */
    void purge() {
        long cutoff = clock.millis() - maxWindowMillis.get();
        attempts.forEach((key, ignored) ->
                attempts.computeIfPresent(key, (k, deque) -> {
                    while (!deque.isEmpty() && deque.peekFirst() < cutoff) {
                        deque.pollFirst();
                    }
                    return deque.isEmpty() ? null : deque;
                }));
    }

    private void trackWindow(Duration window) {
        maxWindowMillis.accumulateAndGet(window.toMillis(), Math::max);
    }

    /**
     * Vacía el estado del limitador. Utilidad de testing: permite reiniciar
     * las ventanas entre tests de integración sin reiniciar el contexto.
     */
    void clear() {
        attempts.clear();
        maxWindowMillis.set(0);
    }

    @Override
    public void close() {
        purgeExecutor.shutdownNow();
    }
}