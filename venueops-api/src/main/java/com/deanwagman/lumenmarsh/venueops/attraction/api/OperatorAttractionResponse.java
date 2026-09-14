package com.deanwagman.lumenmarsh.venueops.attraction.api;

import com.deanwagman.lumenmarsh.venueops.attraction.domain.Attraction;
import com.deanwagman.lumenmarsh.venueops.attraction.domain.AttractionStatus;
import com.deanwagman.lumenmarsh.venueops.attraction.domain.AttractionType;
import com.deanwagman.lumenmarsh.venueops.attraction.domain.CapacityMode;

import java.time.Instant;

public record OperatorAttractionResponse(
        String id,
        String name,
        String area,
        AttractionType type,
        AttractionStatus status,
        CapacityMode capacityMode,
        Integer waitMinutes,
        Instant updatedAt,
        long version
) {
    public static OperatorAttractionResponse from(Attraction attraction) {
        return new OperatorAttractionResponse(
                attraction.id().value(),
                attraction.name(),
                attraction.area(),
                attraction.type(),
                attraction.status(),
                attraction.capacityMode(),
                attraction.waitMinutes(),
                attraction.updatedAt(),
                attraction.version()
        );
    }
}
