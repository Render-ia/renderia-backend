package com.renderia.renderia_backend.security.ratelimit;

import java.time.Clock;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Ventana deslizante en memoria: cuenta intentos por clave (correo, IP...) y
 * bloquea la clave cuando llega al máximo dentro de la ventana.
 *
 * Basta para una sola instancia del backend (el caso de Render). Si se escalara a
 * varias instancias habría que mover el conteo a Redis; la interfaz no cambiaría.
 */
public class AttemptLimiter {

    /** Por encima de este número de claves se limpian las que ya vencieron. */
    private static final int CLEANUP_THRESHOLD = 10_000;

    private final int maxAttempts;
    private final Duration window;
    private final Clock clock;
    private final Map<String, Deque<Long>> attempts = new ConcurrentHashMap<>();

    public AttemptLimiter(int maxAttempts, Duration window, Clock clock) {
        this.maxAttempts = maxAttempts;
        this.window = window;
        this.clock = clock;
    }

    /** Segundos que debe esperar la clave, o 0 si todavía puede intentar. */
    public long secondsUntilAllowed(String key) {
        Deque<Long> times = attempts.get(key);
        if (times == null) {
            return 0;
        }
        synchronized (times) {
            long now = clock.millis();
            prune(times, now);
            if (times.size() < maxAttempts) {
                return 0;
            }
            long oldest = times.peekFirst();
            return Math.max(1, (oldest + window.toMillis() - now + 999) / 1000);
        }
    }

    public void record(String key) {
        long now = clock.millis();
        Deque<Long> times = attempts.computeIfAbsent(key, k -> new ArrayDeque<>());
        synchronized (times) {
            prune(times, now);
            times.addLast(now);
        }
        if (attempts.size() > CLEANUP_THRESHOLD) {
            cleanup(now);
        }
    }

    public void reset(String key) {
        attempts.remove(key);
    }

    private void prune(Deque<Long> times, long now) {
        long limit = now - window.toMillis();
        while (!times.isEmpty() && times.peekFirst() <= limit) {
            times.pollFirst();
        }
    }

    private void cleanup(long now) {
        attempts.entrySet().removeIf(entry -> {
            synchronized (entry.getValue()) {
                prune(entry.getValue(), now);
                return entry.getValue().isEmpty();
            }
        });
    }
}
