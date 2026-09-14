package com.deanwagman.lumenmarsh.venueops.weather.application;

import com.deanwagman.lumenmarsh.venueops.weather.domain.WeatherRecommendation;
import com.deanwagman.lumenmarsh.venueops.weather.domain.WeatherRecommendationId;

import java.util.List;
import java.util.Optional;

public interface WeatherRecommendationRepository {

    Optional<WeatherRecommendation> findById(WeatherRecommendationId id);

    List<WeatherRecommendation> findAll();

    void save(WeatherRecommendation recommendation);
}
