package com.deanwagman.lumenmarsh.venueops.weather.domain;

import com.deanwagman.lumenmarsh.venueops.attraction.domain.AttractionId;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

public record WeatherRecommendationInbound(
        WeatherRecommendationId id,
        String ruleId,
        WeatherRecommendationStatus status,
        WeatherRecommendationSeverity severity,
        String summary,
        String evidence,
        String recommendedAction,
        List<AttractionId> affectedAttractionIds,
        Instant observedAt,
        long sourceVersion
) {
    public WeatherRecommendationInbound {
        Objects.requireNonNull(id, "id is required");
        Objects.requireNonNull(status, "status is required");
        Objects.requireNonNull(severity, "severity is required");
        Objects.requireNonNull(observedAt, "observedAt is required");
        ruleId = requireText(ruleId, "ruleId");
        summary = requireText(summary, "summary");
        evidence = requireText(evidence, "evidence");
        recommendedAction = requireText(recommendedAction, "recommendedAction");
        affectedAttractionIds = affectedAttractionIds == null ? List.of() : List.copyOf(affectedAttractionIds);
        if (sourceVersion < 1) {
            throw new IllegalArgumentException("sourceVersion must be at least 1");
        }
    }

    private static String requireText(String value, String field) {
        Objects.requireNonNull(value, field + " is required");
        if (value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return value.trim();
    }
}
