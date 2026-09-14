package com.deanwagman.lumenmarsh.venueops.attraction.domain;

import java.time.Instant;

public record AttractionCapacityChanged(
        String id,
        AttractionId attractionId,
        AttractionEventType type,
        String actor,
        String reason,
        Instant occurredAt,
        long previousVersion,
        long resultingVersion,
        CapacityMode previousMode,
        CapacityMode newMode
) implements AttractionActivity {
}
