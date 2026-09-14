package com.deanwagman.lumenmarsh.venueops.attraction.api;

import com.deanwagman.lumenmarsh.venueops.attraction.application.GuestAttractionReadService;
import com.deanwagman.lumenmarsh.venueops.attraction.domain.AttractionStatus;
import com.deanwagman.lumenmarsh.venueops.attraction.domain.AttractionType;
import com.deanwagman.lumenmarsh.venueops.attraction.domain.CapacityMode;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(name = "AttractionDetail")
public record AttractionDetailResponse(
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
        AttractionExperienceResponse experience
) {
    public static AttractionDetailResponse from(GuestAttractionReadService.AttractionDetail detail) {
        var operational = detail.operational();
        return new AttractionDetailResponse(
                operational.id().value(),
                operational.name(),
                operational.area(),
                operational.type(),
                operational.status(),
                operational.capacityMode(),
                operational.waitMinutes(),
                operational.guestStatusMessage(),
                operational.updatedAt(),
                operational.version(),
                AttractionExperienceResponse.from(detail.experience())
        );
    }
}
