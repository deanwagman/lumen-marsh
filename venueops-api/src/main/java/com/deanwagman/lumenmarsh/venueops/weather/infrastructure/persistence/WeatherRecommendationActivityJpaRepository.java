package com.deanwagman.lumenmarsh.venueops.weather.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WeatherRecommendationActivityJpaRepository
        extends JpaRepository<WeatherRecommendationActivityEntity, String> {

    List<WeatherRecommendationActivityEntity> findByRecommendationIdOrderByResultingVersionAsc(String recommendationId);
}
