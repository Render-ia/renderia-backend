package com.renderia.renderia_backend.security.quota;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.springframework.http.HttpStatus;

import com.renderia.renderia_backend.exception.ApiException;

/**
 * Counts paid AI calls per user and per UTC day, in memory. It protects the
 * Gemini bill from loops, bugs and abusive accounts; the AI service keeps its
 * own global daily limit on top of this one.
 */
public class DailyQuota {

    private final Clock clock;
    private final ConcurrentMap<String, Usage> usage = new ConcurrentHashMap<>();

    public DailyQuota(Clock clock) {
        this.clock = clock;
    }

    /** Takes one unit for the key, or answers 429 with the given message when the limit is reached. */
    public void take(String key, int limit, String message) {
        LocalDate today = LocalDate.now(clock.withZone(ZoneOffset.UTC));
        Usage next = usage.compute(key, (k, current) -> {
            int used = current == null || !current.day().equals(today) ? 0 : current.count();
            return new Usage(today, used + 1);
        });
        if (next.count() > limit) {
            usage.computeIfPresent(key, (k, current) -> new Usage(current.day(), current.count() - 1));
            throw new ApiException(HttpStatus.TOO_MANY_REQUESTS, message);
        }
    }

    /** Units still available today for the key. */
    public int remaining(String key, int limit) {
        LocalDate today = LocalDate.now(clock.withZone(ZoneOffset.UTC));
        Usage current = usage.get(key);
        int used = current == null || !current.day().equals(today) ? 0 : current.count();
        return Math.max(0, limit - used);
    }

    private record Usage(LocalDate day, int count) {
    }
}
