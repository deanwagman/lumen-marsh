package com.deanwagman.lumenmarsh.venueops.attraction.api;

import com.deanwagman.lumenmarsh.venueops.attraction.domain.Attraction;
import com.deanwagman.lumenmarsh.venueops.attraction.domain.AttractionExperienceProfile;
import com.deanwagman.lumenmarsh.venueops.attraction.domain.AttractionStatus;
import com.deanwagman.lumenmarsh.venueops.attraction.domain.AttractionType;
import com.deanwagman.lumenmarsh.venueops.attraction.domain.CapacityMode;

import java.time.Instant;

public record AttractionResponse(
        String id,
        String name,
        String area,
        AttractionType type,
        AttractionStatus status,
        CapacityMode capacityMode,
        Integer waitMinutes,
        String statusMessage,
        Instant updatedAt,
        long version,
        String thumbnailUrl,
        String thumbnailAltText
) {
    public static AttractionResponse from(Attraction attraction, AttractionExperienceProfile experience) {
        return new AttractionResponse(
                attraction.id().value(),
                attraction.name(),
                attraction.area(),
                attraction.type(),
                attraction.status(),
                attraction.capacityMode(),
                attraction.waitMinutes(),
                attraction.guestStatusMessage(),
                attraction.updatedAt(),
                attraction.version(),
                experience.media().thumbnailUrl(),
                experience.media().altText()
        );
    }
}
