package com.deanwagman.lumenmarsh.venueops.attraction.api;

import com.deanwagman.lumenmarsh.venueops.attraction.domain.AttractionActivity;
import com.deanwagman.lumenmarsh.venueops.attraction.domain.AttractionCapacityChanged;
import com.deanwagman.lumenmarsh.venueops.attraction.domain.AttractionEventType;
import com.deanwagman.lumenmarsh.venueops.attraction.domain.AttractionStatus;
import com.deanwagman.lumenmarsh.venueops.attraction.domain.AttractionStatusChanged;
import com.deanwagman.lumenmarsh.venueops.attraction.domain.AttractionWaitTimeChanged;
import com.deanwagman.lumenmarsh.venueops.attraction.domain.CapacityMode;

import java.time.Instant;

public record AttractionActivityResponse(
        String id,
        String attractionId,
        AttractionEventType type,
        String actor,
        String reason,
        Instant occurredAt,
        long previousVersion,
        long resultingVersion,
        AttractionStatus previousStatus,
        AttractionStatus newStatus,
        CapacityMode previousMode,
        CapacityMode newMode,
        Integer previousMinutes,
        Integer newMinutes
) {
    public static AttractionActivityResponse from(AttractionActivity activity) {
        return switch (activity) {
            case AttractionStatusChanged changed -> new AttractionActivityResponse(
                    changed.id(),
                    changed.attractionId().value(),
                    changed.type(),
                    changed.actor(),
                    changed.reason(),
                    changed.occurredAt(),
                    changed.previousVersion(),
                    changed.resultingVersion(),
                    changed.previousStatus(),
                    changed.newStatus(),
                    null,
                    null,
                    null,
                    null
            );
            case AttractionCapacityChanged changed -> new AttractionActivityResponse(
                    changed.id(),
                    changed.attractionId().value(),
                    changed.type(),
                    changed.actor(),
                    changed.reason(),
                    changed.occurredAt(),
                    changed.previousVersion(),
                    changed.resultingVersion(),
                    null,
                    null,
                    changed.previousMode(),
                    changed.newMode(),
                    null,
                    null
            );
            case AttractionWaitTimeChanged changed -> new AttractionActivityResponse(
                    changed.id(),
                    changed.attractionId().value(),
                    changed.type(),
                    changed.actor(),
                    changed.reason(),
                    changed.occurredAt(),
                    changed.previousVersion(),
                    changed.resultingVersion(),
                    null,
                    null,
                    null,
                    null,
                    changed.previousMinutes(),
                    changed.newMinutes()
            );
        };
    }
}
