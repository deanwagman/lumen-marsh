package com.deanwagman.lumenmarsh.venueops.attraction.application;

import com.deanwagman.lumenmarsh.venueops.attraction.domain.Attraction;
import com.deanwagman.lumenmarsh.venueops.attraction.domain.AttractionStatus;
import com.deanwagman.lumenmarsh.venueops.attraction.domain.CapacityMode;

import java.time.Instant;

public record AttractionOperationalSnapshot(
        String id,
        AttractionStatus status,
        CapacityMode capacityMode,
        Integer waitMinutes,
        String statusMessage,
        Instant updatedAt,
        long version
) {
    public static AttractionOperationalSnapshot from(Attraction attraction) {
        return new AttractionOperationalSnapshot(
                attraction.id().value(),
                attraction.status(),
                attraction.capacityMode(),
                attraction.waitMinutes(),
                attraction.guestStatusMessage(),
                attraction.updatedAt(),
                attraction.version()
        );
    }
}
