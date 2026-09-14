package com.deanwagman.lumenmarsh.venueops.attraction.domain;

import java.time.Instant;

public record AttractionWaitTimeChanged(
        String id,
        AttractionId attractionId,
        AttractionEventType type,
        String actor,
        String reason,
        Instant occurredAt,
        long previousVersion,
        long resultingVersion,
        Integer previousMinutes,
        Integer newMinutes
) implements AttractionActivity {
}
