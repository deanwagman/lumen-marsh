package com.deanwagman.lumenmarsh.venueops.security;

import java.time.Clock;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Fixed one-minute window limiter for the weather ingest endpoint.
 */
public final class WeatherIngestRateLimiter {

    private static final long WINDOW_MILLIS = Duration.ofMinutes(1).toMillis();

    private final int limitPerMinute;
    private final Clock clock;
    private final ConcurrentHashMap<String, Window> windows = new ConcurrentHashMap<>();

    public WeatherIngestRateLimiter(int limitPerMinute, Clock clock) {
        if (limitPerMinute < 0) {
            throw new IllegalArgumentException("limitPerMinute must be >= 0");
        }
        this.limitPerMinute = limitPerMinute;
        this.clock = clock;
    }

    public boolean isDisabled() {
        return limitPerMinute == 0;
    }

    public boolean tryAcquire(String key) {
        if (isDisabled()) {
            return true;
        }
        long now = clock.millis();
        Window window = windows.compute(key, (ignored, existing) -> {
            if (existing == null || now - existing.startMillis >= WINDOW_MILLIS) {
                return new Window(now);
            }
            existing.count.incrementAndGet();
            return existing;
        });
        return window.count.get() <= limitPerMinute;
    }

    private static final class Window {
        private final long startMillis;
        private final AtomicInteger count = new AtomicInteger(1);

        private Window(long startMillis) {
            this.startMillis = startMillis;
        }
    }
}
