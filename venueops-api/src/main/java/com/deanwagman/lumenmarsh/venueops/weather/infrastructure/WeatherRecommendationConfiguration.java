package com.deanwagman.lumenmarsh.venueops.weather.infrastructure;

import com.deanwagman.lumenmarsh.venueops.incident.application.IncidentRepository;
import com.deanwagman.lumenmarsh.venueops.weather.application.WeatherRecommendationRepository;
import com.deanwagman.lumenmarsh.venueops.weather.application.WeatherRecommendationService;
import com.deanwagman.lumenmarsh.venueops.weather.application.WeatherRecommendationUpdatePublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class WeatherRecommendationConfiguration {

    @Bean
    WeatherRecommendationService weatherRecommendationService(
            WeatherRecommendationRepository recommendations,
            IncidentRepository incidents,
            Clock clock,
            WeatherRecommendationUpdatePublisher publisher
    ) {
        return new WeatherRecommendationService(recommendations, incidents, clock, publisher);
    }
}
