package com.deanwagman.lumenmarsh.venueops.weather.api;

import com.deanwagman.lumenmarsh.venueops.weather.application.WeatherRecommendationOperationalSnapshot;
import com.deanwagman.lumenmarsh.venueops.weather.domain.WeatherRecommendation;
import com.deanwagman.lumenmarsh.venueops.weather.domain.WeatherRecommendationOperatorStatus;
import com.deanwagman.lumenmarsh.venueops.weather.domain.WeatherRecommendationSeverity;
import com.deanwagman.lumenmarsh.venueops.weather.domain.WeatherRecommendationStatus;

import java.time.Instant;
import java.util.List;

public record OperatorWeatherRecommendationResponse(
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
    public static OperatorWeatherRecommendationResponse from(WeatherRecommendation recommendation) {
        return from(WeatherRecommendationOperationalSnapshot.from(recommendation));
    }

    public static OperatorWeatherRecommendationResponse from(WeatherRecommendationOperationalSnapshot snapshot) {
        return new OperatorWeatherRecommendationResponse(
                snapshot.id(),
                snapshot.ruleId(),
                snapshot.status(),
                snapshot.operatorStatus(),
                snapshot.severity(),
                snapshot.summary(),
                snapshot.evidence(),
                snapshot.recommendedAction(),
                snapshot.affectedAttractionIds(),
                snapshot.observedAt(),
                snapshot.receivedAt(),
                snapshot.updatedAt(),
                snapshot.sourceVersion(),
                snapshot.version(),
                snapshot.simulated(),
                snapshot.linkedIncidentId()
        );
    }
}
