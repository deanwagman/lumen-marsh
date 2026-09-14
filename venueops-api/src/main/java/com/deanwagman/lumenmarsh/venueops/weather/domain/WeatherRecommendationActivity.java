package com.deanwagman.lumenmarsh.venueops.weather.domain;

import com.deanwagman.lumenmarsh.venueops.incident.domain.IncidentId;

import java.time.Instant;
import java.util.Objects;

public record WeatherRecommendationActivity(
        String id,
        WeatherRecommendationId recommendationId,
        WeatherRecommendationEventType type,
        String actor,
        String reason,
        Instant occurredAt,
        long previousVersion,
        long resultingVersion,
        long sourceVersion,
        IncidentId linkedIncidentId
) {
    public WeatherRecommendationActivity {
        Objects.requireNonNull(id, "id is required");
        Objects.requireNonNull(recommendationId, "recommendationId is required");
        Objects.requireNonNull(type, "type is required");
        Objects.requireNonNull(actor, "actor is required");
        Objects.requireNonNull(occurredAt, "occurredAt is required");
    }
}
