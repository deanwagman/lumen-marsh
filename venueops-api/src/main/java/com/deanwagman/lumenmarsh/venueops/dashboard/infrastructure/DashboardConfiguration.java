package com.deanwagman.lumenmarsh.venueops.dashboard.infrastructure;

import com.deanwagman.lumenmarsh.venueops.attraction.application.AttractionService;
import com.deanwagman.lumenmarsh.venueops.dashboard.application.OperatorDashboardService;
import com.deanwagman.lumenmarsh.venueops.flow.application.FlowRecommendationService;
import com.deanwagman.lumenmarsh.venueops.incident.application.IncidentService;
import com.deanwagman.lumenmarsh.venueops.maintenance.application.MaintenanceWorkOrderRepository;
import com.deanwagman.lumenmarsh.venueops.weather.application.WeatherRecommendationService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class DashboardConfiguration {

    @Bean
    OperatorDashboardService operatorDashboardService(
            AttractionService attractions,
            IncidentService incidents,
            WeatherRecommendationService weatherRecommendations,
            MaintenanceWorkOrderRepository workOrders,
            FlowRecommendationService flowRecommendations,
            Clock clock
    ) {
        return new OperatorDashboardService(
                attractions,
                incidents,
                weatherRecommendations,
                workOrders,
                flowRecommendations,
                clock
        );
    }
}
