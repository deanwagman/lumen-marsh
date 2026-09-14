package com.deanwagman.lumenmarsh.venueops.weather.application;

import com.deanwagman.lumenmarsh.venueops.weather.domain.WeatherRecommendation;

public record WeatherRecommendationIngestResult(
        WeatherRecommendation recommendation,
        boolean created,
        boolean duplicate
) {
    public static WeatherRecommendationIngestResult created(WeatherRecommendation recommendation) {
        return new WeatherRecommendationIngestResult(recommendation, true, false);
    }

    public static WeatherRecommendationIngestResult duplicate(WeatherRecommendation recommendation) {
        return new WeatherRecommendationIngestResult(recommendation, false, true);
    }

    public static WeatherRecommendationIngestResult updated(WeatherRecommendation recommendation) {
        return new WeatherRecommendationIngestResult(recommendation, false, false);
    }
}
