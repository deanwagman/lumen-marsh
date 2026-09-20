package com.deanwagman.lumenmarsh.venueops.flow.application;

import com.deanwagman.lumenmarsh.venueops.attraction.domain.AttractionId;
import com.deanwagman.lumenmarsh.venueops.flow.domain.QueueForecast;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface QueueForecastRepository {

    Optional<QueueForecast> findById(UUID forecastId);

    List<QueueForecast> findLatestByAttraction(AttractionId attractionId);

    void save(QueueForecast forecast);
}
