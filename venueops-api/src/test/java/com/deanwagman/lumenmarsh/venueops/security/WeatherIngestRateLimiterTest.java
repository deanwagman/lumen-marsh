package com.deanwagman.lumenmarsh.venueops.security;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.TemporalAmount;

import static org.assertj.core.api.Assertions.assertThat;

class WeatherIngestRateLimiterTest {

    @Test
    void rejectsAfterLimitThenAllowsNextWindow() {
        TickClock clock = new TickClock(Instant.parse("2026-09-03T16:00:00Z"));
        WeatherIngestRateLimiter limiter = new WeatherIngestRateLimiter(2, clock);

        assertThat(limiter.tryAcquire("10.0.0.1")).isTrue();
        assertThat(limiter.tryAcquire("10.0.0.1")).isTrue();
        assertThat(limiter.tryAcquire("10.0.0.1")).isFalse();
        assertThat(limiter.tryAcquire("10.0.0.2")).isTrue();

        clock.advance(Duration.ofMinutes(1));
        assertThat(limiter.tryAcquire("10.0.0.1")).isTrue();
    }

    @Test
    void zeroLimitDisablesLimiter() {
        WeatherIngestRateLimiter limiter = new WeatherIngestRateLimiter(0, Clock.systemUTC());
        assertThat(limiter.isDisabled()).isTrue();
        assertThat(limiter.tryAcquire("any")).isTrue();
        assertThat(limiter.tryAcquire("any")).isTrue();
    }

    private static final class TickClock extends Clock {
        private Instant instant;

        private TickClock(Instant instant) {
            this.instant = instant;
        }

        void advance(TemporalAmount amount) {
            instant = instant.plus(amount);
        }

        @Override
        public ZoneOffset getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(java.time.ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return instant;
        }
    }
}
