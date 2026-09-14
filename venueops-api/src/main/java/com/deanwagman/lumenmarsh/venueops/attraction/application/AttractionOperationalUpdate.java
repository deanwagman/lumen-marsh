package com.deanwagman.lumenmarsh.venueops.attraction.application;

import com.deanwagman.lumenmarsh.venueops.attraction.domain.Attraction;
import com.deanwagman.lumenmarsh.venueops.attraction.domain.AttractionActivity;

import java.time.Instant;

public record AttractionOperationalUpdate(
        String eventId,
        AttractionUpdateEventType eventType,
        Instant occurredAt,
        AttractionOperationalSnapshot attraction
) {
    public static AttractionOperationalUpdate from(AttractionActivity activity, Attraction attraction) {
        return new AttractionOperationalUpdate(
                activity.id(),
                AttractionUpdateEventType.from(activity),
                activity.occurredAt(),
                AttractionOperationalSnapshot.from(attraction)
        );
    }
}
