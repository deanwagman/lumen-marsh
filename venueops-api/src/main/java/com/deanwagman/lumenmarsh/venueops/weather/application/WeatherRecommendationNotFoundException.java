package com.deanwagman.lumenmarsh.venueops.weather.application;

import com.deanwagman.lumenmarsh.venueops.weather.domain.WeatherRecommendationId;

public class WeatherRecommendationNotFoundException extends RuntimeException {

    private final WeatherRecommendationId recommendationId;

    public WeatherRecommendationNotFoundException(WeatherRecommendationId recommendationId) {
        super("Weather recommendation not found: " + recommendationId);
        this.recommendationId = recommendationId;
    }

    public WeatherRecommendationId recommendationId() {
        return recommendationId;
    }
}
