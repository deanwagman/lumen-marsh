package com.deanwagman.lumenmarsh.venueops.weather.domain;

public class StaleWeatherRecommendationSourceException extends RuntimeException {

    private final WeatherRecommendationId recommendationId;
    private final long inboundVersion;
    private final long currentSourceVersion;

    public StaleWeatherRecommendationSourceException(
            WeatherRecommendationId recommendationId,
            long inboundVersion,
            long currentSourceVersion
    ) {
        super("Weather recommendation " + recommendationId
                + " inbound source version " + inboundVersion
                + " is older than stored " + currentSourceVersion);
        this.recommendationId = recommendationId;
        this.inboundVersion = inboundVersion;
        this.currentSourceVersion = currentSourceVersion;
    }

    public WeatherRecommendationId recommendationId() {
        return recommendationId;
    }

    public long inboundVersion() {
        return inboundVersion;
    }

    public long currentSourceVersion() {
        return currentSourceVersion;
    }
}
