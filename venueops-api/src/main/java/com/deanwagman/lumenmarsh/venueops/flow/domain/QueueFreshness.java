package com.deanwagman.lumenmarsh.venueops.flow.domain;

import java.time.Duration;
import java.time.Instant;

public enum QueueFreshness {
    FRESH,
    DELAYED,
    STALE;

    public static final Duration FRESH_LIMIT = Duration.ofMinutes(2);
    public static final Duration DELAYED_LIMIT = Duration.ofMinutes(5);

    public static QueueFreshness of(Instant observedAt, Instant now) {
        if (observedAt == null || now == null) {
            return STALE;
        }
        Duration age = Duration.between(observedAt, now);
        if (age.isNegative() || age.compareTo(FRESH_LIMIT) <= 0) {
            return FRESH;
        }
        if (age.compareTo(DELAYED_LIMIT) <= 0) {
            return DELAYED;
        }
        return STALE;
    }
}
