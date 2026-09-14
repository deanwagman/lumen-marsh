package com.deanwagman.lumenmarsh.venueops.weather.infrastructure.persistence;

import java.io.Serializable;
import java.util.Objects;

public final class WeatherRecommendationAttractionId implements Serializable {

    private String recommendationId;
    private String attractionId;

    public WeatherRecommendationAttractionId() {
    }

    public WeatherRecommendationAttractionId(String recommendationId, String attractionId) {
        this.recommendationId = recommendationId;
        this.attractionId = attractionId;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof WeatherRecommendationAttractionId that)) {
            return false;
        }
        return Objects.equals(recommendationId, that.recommendationId)
                && Objects.equals(attractionId, that.attractionId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(recommendationId, attractionId);
    }
}
