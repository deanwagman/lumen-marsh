package com.deanwagman.lumenmarsh.venueops.weather.application;

import com.deanwagman.lumenmarsh.venueops.weather.domain.WeatherRecommendation;
import com.deanwagman.lumenmarsh.venueops.weather.domain.WeatherRecommendationActivity;
import com.deanwagman.lumenmarsh.venueops.weather.domain.WeatherRecommendationEventType;
import com.deanwagman.lumenmarsh.venueops.weather.domain.WeatherRecommendationStatus;

import java.time.Instant;

public record WeatherRecommendationOperationalUpdate(
        String eventId,
        WeatherRecommendationUpdateEventType eventType,
        Instant occurredAt,
        WeatherRecommendationOperationalSnapshot recommendation
) {
    public static final String UPDATED_EVENT = "weather.recommendation.updated";
    public static final String CLEARED_EVENT = "weather.recommendation.cleared";
    public static final String SNAPSHOT_EVENT = "weather.recommendations.snapshot";

    public String sseEventName() {
        return switch (eventType) {
            case UPDATED -> UPDATED_EVENT;
            case CLEARED -> CLEARED_EVENT;
        };
    }

    public static WeatherRecommendationOperationalUpdate fromActivity(
            WeatherRecommendationActivity activity,
            WeatherRecommendation recommendation
    ) {
        boolean sourceCleared = recommendation.status() == WeatherRecommendationStatus.CLEARED
                && (activity.type() == WeatherRecommendationEventType.RECEIVED
                || activity.type() == WeatherRecommendationEventType.SOURCE_UPDATED);
        return new WeatherRecommendationOperationalUpdate(
                activity.id(),
                sourceCleared
                        ? WeatherRecommendationUpdateEventType.CLEARED
                        : WeatherRecommendationUpdateEventType.UPDATED,
                activity.occurredAt(),
                WeatherRecommendationOperationalSnapshot.from(recommendation)
        );
    }
}
