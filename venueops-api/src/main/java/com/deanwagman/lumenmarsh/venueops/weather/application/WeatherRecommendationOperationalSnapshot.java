package com.deanwagman.lumenmarsh.venueops.weather.application;

import com.deanwagman.lumenmarsh.venueops.weather.domain.WeatherRecommendation;
import com.deanwagman.lumenmarsh.venueops.weather.domain.WeatherRecommendationOperatorStatus;
import com.deanwagman.lumenmarsh.venueops.weather.domain.WeatherRecommendationSeverity;
import com.deanwagman.lumenmarsh.venueops.weather.domain.WeatherRecommendationStatus;

import java.time.Instant;
import java.util.List;

public record WeatherRecommendationOperationalSnapshot(
        String id,
        String ruleId,
        WeatherRecommendationStatus status,
        WeatherRecommendationOperatorStatus operatorStatus,
        WeatherRecommendationSeverity severity,
        String summary,
        String evidence,
        String recommendedAction,
        List<String> affectedAttractionIds,
        Instant observedAt,
        Instant receivedAt,
        Instant updatedAt,
        long sourceVersion,
        long version,
        boolean simulated,
        String linkedIncidentId
) {
    public static WeatherRecommendationOperationalSnapshot from(WeatherRecommendation recommendation) {
        return new WeatherRecommendationOperationalSnapshot(
                recommendation.id().value(),
                recommendation.ruleId(),
                recommendation.status(),
                recommendation.operatorStatus(),
                recommendation.severity(),
                recommendation.summary(),
                recommendation.evidence(),
                recommendation.recommendedAction(),
                recommendation.affectedAttractionIds().stream().map(id -> id.value()).toList(),
                recommendation.observedAt(),
                recommendation.receivedAt(),
                recommendation.updatedAt(),
                recommendation.sourceVersion(),
                recommendation.version(),
                recommendation.simulated(),
                recommendation.linkedIncidentId() == null ? null : recommendation.linkedIncidentId().value()
        );
    }
}
