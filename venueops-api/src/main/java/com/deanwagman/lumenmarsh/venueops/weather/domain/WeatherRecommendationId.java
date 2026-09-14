package com.deanwagman.lumenmarsh.venueops.weather.domain;

import java.util.Objects;

public record WeatherRecommendationId(String value) {

    public WeatherRecommendationId {
        Objects.requireNonNull(value, "Recommendation id is required");
        if (value.isBlank()) {
            throw new IllegalArgumentException("Recommendation id must not be blank");
        }
    }

    @Override
    public String toString() {
        return value;
    }
}
