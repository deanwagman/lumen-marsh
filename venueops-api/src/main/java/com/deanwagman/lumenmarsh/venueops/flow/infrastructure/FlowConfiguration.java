package com.deanwagman.lumenmarsh.venueops.flow.infrastructure;

import com.deanwagman.lumenmarsh.venueops.attraction.application.AttractionRepository;
import com.deanwagman.lumenmarsh.venueops.flow.application.FlowForecastService;
import com.deanwagman.lumenmarsh.venueops.flow.application.FlowMetrics;
import com.deanwagman.lumenmarsh.venueops.flow.application.FlowObservationRepository;
import com.deanwagman.lumenmarsh.venueops.flow.application.FlowObservationService;
import com.deanwagman.lumenmarsh.venueops.flow.application.FlowProcessedCommandRepository;
import com.deanwagman.lumenmarsh.venueops.flow.application.FlowQueryService;
import com.deanwagman.lumenmarsh.venueops.flow.application.FlowRecommendationRepository;
import com.deanwagman.lumenmarsh.venueops.flow.application.FlowRecommendationService;
import com.deanwagman.lumenmarsh.venueops.flow.application.FlowRelatedOperations;
import com.deanwagman.lumenmarsh.venueops.flow.application.FlowUpdatePublisher;
import com.deanwagman.lumenmarsh.venueops.incident.application.IncidentRepository;
import com.deanwagman.lumenmarsh.venueops.maintenance.application.MaintenanceWorkOrderRepository;
import com.deanwagman.lumenmarsh.venueops.flow.application.QueueForecastRepository;
import com.deanwagman.lumenmarsh.venueops.flow.application.QueueProjectionRepository;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class FlowConfiguration {

    @Bean
    FlowMetrics flowMetrics(
            MeterRegistry meterRegistry,
            QueueProjectionRepository projections,
            FlowRecommendationRepository recommendations,
            Clock clock
    ) {
        return new FlowMetrics(meterRegistry, projections, recommendations, clock);
    }

    @Bean
    FlowObservationService flowObservationService(
            FlowObservationRepository observations,
            QueueProjectionRepository projections,
            AttractionRepository attractions,
            FlowUpdatePublisher publisher,
            FlowMetrics metrics,
            Clock clock
    ) {
        return new FlowObservationService(observations, projections, attractions, publisher, metrics, clock);
    }

    @Bean
    FlowRelatedOperations flowRelatedOperations(
            IncidentRepository incidents,
            MaintenanceWorkOrderRepository workOrders
    ) {
        return new FlowRelatedOperations(incidents, workOrders);
    }

    @Bean
    FlowForecastService flowForecastService(
            QueueForecastRepository forecasts,
            FlowObservationRepository observations,
            QueueProjectionRepository projections,
            AttractionRepository attractions,
            FlowRecommendationRepository recommendations,
            FlowUpdatePublisher publisher,
            FlowRelatedOperations relatedOperations,
            Clock clock
    ) {
        return new FlowForecastService(
                forecasts,
                observations,
                projections,
                attractions,
                recommendations,
                publisher,
                relatedOperations,
                clock
        );
    }

    @Bean
    FlowRecommendationService flowRecommendationService(
            FlowRecommendationRepository recommendations,
            FlowProcessedCommandRepository processedCommands,
            FlowUpdatePublisher publisher,
            FlowMetrics metrics,
            Clock clock
    ) {
        return new FlowRecommendationService(recommendations, processedCommands, publisher, metrics, clock);
    }

    @Bean
    FlowQueryService flowQueryService(
            AttractionRepository attractions,
            QueueProjectionRepository projections,
            QueueForecastRepository forecasts,
            FlowObservationRepository observations,
            FlowRecommendationService recommendations,
            Clock clock
    ) {
        return new FlowQueryService(attractions, projections, forecasts, observations, recommendations, clock);
    }
}
