package com.deanwagman.lumenmarsh.venueops.weather.infrastructure.persistence;

import com.deanwagman.lumenmarsh.venueops.incident.domain.IncidentId;
import com.deanwagman.lumenmarsh.venueops.weather.domain.WeatherRecommendationActivity;
import com.deanwagman.lumenmarsh.venueops.weather.domain.WeatherRecommendationId;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.util.LinkedHashMap;
import java.util.Map;

final class WeatherRecommendationActivityMapper {

    private static final JsonMapper JSON = JsonMapper.builder().build();

    private WeatherRecommendationActivityMapper() {
    }

    static WeatherRecommendationActivityEntity toEntity(WeatherRecommendationActivity activity) {
        return new WeatherRecommendationActivityEntity(
                activity.id(),
                activity.recommendationId().value(),
                activity.type(),
                activity.actor(),
                activity.reason(),
                activity.occurredAt(),
                activity.previousVersion(),
                activity.resultingVersion(),
                JSON.writeValueAsString(payload(activity))
        );
    }

    static WeatherRecommendationActivity toDomain(WeatherRecommendationActivityEntity entity) {
        JsonNode payload = JSON.readTree(entity.getPayload());
        IncidentId linked = null;
        if (payload.hasNonNull("linkedIncidentId")) {
            linked = new IncidentId(payload.get("linkedIncidentId").asString());
        }
        return new WeatherRecommendationActivity(
                entity.getId(),
                new WeatherRecommendationId(entity.getRecommendationId()),
                entity.getType(),
                entity.getActor(),
                entity.getReason(),
                entity.getOccurredAt(),
                entity.getPreviousVersion(),
                entity.getResultingVersion(),
                payload.get("sourceVersion").asLong(),
                linked
        );
    }

    private static Map<String, Object> payload(WeatherRecommendationActivity activity) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("sourceVersion", activity.sourceVersion());
        payload.put(
                "linkedIncidentId",
                activity.linkedIncidentId() == null ? null : activity.linkedIncidentId().value()
        );
        return payload;
    }
}
