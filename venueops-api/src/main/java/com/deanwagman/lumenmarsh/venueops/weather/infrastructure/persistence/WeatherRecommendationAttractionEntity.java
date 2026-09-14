package com.deanwagman.lumenmarsh.venueops.weather.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

@Entity
@Table(name = "weather_recommendation_attraction")
@IdClass(WeatherRecommendationAttractionId.class)
public class WeatherRecommendationAttractionEntity {

    @Id
    @Column(name = "recommendation_id", length = 36)
    private String recommendationId;

    @Id
    @Column(name = "attraction_id", length = 64)
    private String attractionId;

    protected WeatherRecommendationAttractionEntity() {
    }

    public WeatherRecommendationAttractionEntity(String recommendationId, String attractionId) {
        this.recommendationId = recommendationId;
        this.attractionId = attractionId;
    }

    public String getRecommendationId() {
        return recommendationId;
    }

    public String getAttractionId() {
        return attractionId;
    }
}
