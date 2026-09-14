package com.deanwagman.lumenmarsh.venueops.weather.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface WeatherRecommendationAttractionJpaRepository
        extends JpaRepository<WeatherRecommendationAttractionEntity, WeatherRecommendationAttractionId> {

    List<WeatherRecommendationAttractionEntity> findByRecommendationIdOrderByAttractionIdAsc(String recommendationId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from WeatherRecommendationAttractionEntity l where l.recommendationId = :recommendationId")
    void deleteByRecommendationId(@Param("recommendationId") String recommendationId);
}
