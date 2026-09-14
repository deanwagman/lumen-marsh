package com.deanwagman.lumenmarsh.venueops.attraction.infrastructure.persistence;

import com.deanwagman.lumenmarsh.venueops.attraction.domain.AttractionActivity;
import com.deanwagman.lumenmarsh.venueops.attraction.domain.AttractionCapacityChanged;
import com.deanwagman.lumenmarsh.venueops.attraction.domain.AttractionId;
import com.deanwagman.lumenmarsh.venueops.attraction.domain.AttractionStatus;
import com.deanwagman.lumenmarsh.venueops.attraction.domain.AttractionStatusChanged;
import com.deanwagman.lumenmarsh.venueops.attraction.domain.AttractionWaitTimeChanged;
import com.deanwagman.lumenmarsh.venueops.attraction.domain.CapacityMode;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.util.LinkedHashMap;
import java.util.Map;

final class AttractionActivityMapper {

    private static final JsonMapper JSON = JsonMapper.builder().build();

    private AttractionActivityMapper() {
    }

    static AttractionActivityEntity toEntity(AttractionActivity activity) {
        return new AttractionActivityEntity(
                activity.id(),
                activity.attractionId().value(),
                activity.type(),
                activity.actor(),
                activity.reason(),
                activity.occurredAt(),
                activity.previousVersion(),
                activity.resultingVersion(),
                JSON.writeValueAsString(payload(activity))
        );
    }

    static AttractionActivity toDomain(AttractionActivityEntity entity) {
        JsonNode payload = JSON.readTree(entity.getPayload());
        AttractionId attractionId = new AttractionId(entity.getAttractionId());
        return switch (entity.getType()) {
            case ATTRACTION_TESTING_STARTED,
                 ATTRACTION_TESTING_COMPLETED,
                 ATTRACTION_OPENED,
                 WEATHER_HOLD_PLACED,
                 WEATHER_HOLD_CLEARED,
                 TECHNICAL_FAULT_REPORTED,
                 REPAIR_COMPLETED,
                 ATTRACTION_CLOSED -> new AttractionStatusChanged(
                    entity.getId(),
                    attractionId,
                    entity.getType(),
                    entity.getActor(),
                    entity.getReason(),
                    entity.getOccurredAt(),
                    entity.getPreviousVersion(),
                    entity.getResultingVersion(),
                    AttractionStatus.valueOf(payload.get("previousStatus").asString()),
                    AttractionStatus.valueOf(payload.get("newStatus").asString())
            );
            case CAPACITY_REDUCED, CAPACITY_RESTORED -> new AttractionCapacityChanged(
                    entity.getId(),
                    attractionId,
                    entity.getType(),
                    entity.getActor(),
                    entity.getReason(),
                    entity.getOccurredAt(),
                    entity.getPreviousVersion(),
                    entity.getResultingVersion(),
                    CapacityMode.valueOf(payload.get("previousMode").asString()),
                    CapacityMode.valueOf(payload.get("newMode").asString())
            );
            case WAIT_TIME_UPDATED -> new AttractionWaitTimeChanged(
                    entity.getId(),
                    attractionId,
                    entity.getType(),
                    entity.getActor(),
                    entity.getReason(),
                    entity.getOccurredAt(),
                    entity.getPreviousVersion(),
                    entity.getResultingVersion(),
                    intOrNull(payload.get("previousMinutes")),
                    intOrNull(payload.get("newMinutes"))
            );
        };
    }

    private static Map<String, Object> payload(AttractionActivity activity) {
        Map<String, Object> payload = new LinkedHashMap<>();
        switch (activity) {
            case AttractionStatusChanged changed -> {
                payload.put("previousStatus", changed.previousStatus().name());
                payload.put("newStatus", changed.newStatus().name());
            }
            case AttractionCapacityChanged changed -> {
                payload.put("previousMode", changed.previousMode().name());
                payload.put("newMode", changed.newMode().name());
            }
            case AttractionWaitTimeChanged changed -> {
                payload.put("previousMinutes", changed.previousMinutes());
                payload.put("newMinutes", changed.newMinutes());
            }
        }
        return payload;
    }

    private static Integer intOrNull(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        return node.asInt();
    }
}
