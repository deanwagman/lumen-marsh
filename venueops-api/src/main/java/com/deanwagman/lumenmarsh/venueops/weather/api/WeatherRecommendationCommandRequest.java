package com.deanwagman.lumenmarsh.venueops.weather.api;

import com.deanwagman.lumenmarsh.venueops.weather.domain.WeatherRecommendationCommand;
import jakarta.validation.constraints.NotNull;

public record WeatherRecommendationCommandRequest(
        @NotNull WeatherRecommendationCommand type,
        String reason,
        @NotNull Long expectedVersion,
        String incidentId
) {
}
