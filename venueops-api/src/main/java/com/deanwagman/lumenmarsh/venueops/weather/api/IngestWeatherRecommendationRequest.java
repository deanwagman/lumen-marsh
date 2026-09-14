package com.deanwagman.lumenmarsh.venueops.weather.api;

import com.deanwagman.lumenmarsh.venueops.weather.domain.WeatherRecommendationSeverity;
import com.deanwagman.lumenmarsh.venueops.weather.domain.WeatherRecommendationStatus;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.List;

public record IngestWeatherRecommendationRequest(
        @NotBlank String recommendationId,
        @NotBlank String ruleId,
        @NotNull WeatherRecommendationStatus status,
        @NotNull WeatherRecommendationSeverity severity,
        @NotBlank String summary,
        @NotBlank String evidence,
        @NotBlank String recommendedAction,
        List<String> affectedAttractionIds,
        @NotNull Instant observedAt,
        @NotNull @Min(1) Long version
) {
}
