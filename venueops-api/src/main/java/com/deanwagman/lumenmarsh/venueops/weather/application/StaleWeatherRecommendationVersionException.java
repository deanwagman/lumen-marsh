package com.deanwagman.lumenmarsh.venueops.weather.application;

import com.deanwagman.lumenmarsh.venueops.weather.domain.WeatherRecommendationId;

public class StaleWeatherRecommendationVersionException extends RuntimeException {

    private final WeatherRecommendationId recommendationId;
    private final long expectedVersion;
    private final long actualVersion;

    public StaleWeatherRecommendationVersionException(
            WeatherRecommendationId recommendationId,
            long expectedVersion,
            long actualVersion
    ) {
        super("Weather recommendation " + recommendationId
                + " expected version " + expectedVersion
                + " but was " + actualVersion);
        this.recommendationId = recommendationId;
        this.expectedVersion = expectedVersion;
        this.actualVersion = actualVersion;
    }

    public WeatherRecommendationId recommendationId() {
        return recommendationId;
    }

    public long expectedVersion() {
        return expectedVersion;
    }

    public long actualVersion() {
        return actualVersion;
    }
}
