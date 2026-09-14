package com.deanwagman.lumenmarsh.venueops.attraction.domain;

import java.time.Instant;

public record AttractionStatusChanged(
        String id,
        AttractionId attractionId,
        AttractionEventType type,
        String actor,
        String reason,
        Instant occurredAt,
        long previousVersion,
        long resultingVersion,
        AttractionStatus previousStatus,
        AttractionStatus newStatus
) implements AttractionActivity {
}
