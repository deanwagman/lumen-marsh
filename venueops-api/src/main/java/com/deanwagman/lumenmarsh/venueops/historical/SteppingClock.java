package com.deanwagman.lumenmarsh.venueops.historical;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Objects;

public final class SteppingClock extends Clock {

    private Instant instant;
    private final ZoneId zone;

    public SteppingClock(Instant initialInstant) {
        this(initialInstant, ZoneId.of("UTC"));
    }

    private SteppingClock(Instant instant, ZoneId zone) {
        this.instant = Objects.requireNonNull(instant);
        this.zone = Objects.requireNonNull(zone);
    }

    public void set(Instant instant) {
        this.instant = Objects.requireNonNull(instant);
    }

    @Override
    public ZoneId getZone() {
        return zone;
    }

    @Override
    public Clock withZone(ZoneId zone) {
        return new SteppingClock(instant, zone);
    }

    @Override
    public Instant instant() {
        return instant;
    }
}
