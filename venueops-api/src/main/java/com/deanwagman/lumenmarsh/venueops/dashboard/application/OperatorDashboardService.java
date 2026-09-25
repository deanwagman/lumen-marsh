package com.deanwagman.lumenmarsh.venueops.dashboard.application;

import com.deanwagman.lumenmarsh.venueops.attraction.application.AttractionService;
import com.deanwagman.lumenmarsh.venueops.dashboard.api.OperatorDashboardResponse;
import com.deanwagman.lumenmarsh.venueops.flow.application.FlowRecommendationService;
import com.deanwagman.lumenmarsh.venueops.incident.application.IncidentService;
import com.deanwagman.lumenmarsh.venueops.maintenance.application.MaintenanceWorkOrderRepository;
import com.deanwagman.lumenmarsh.venueops.weather.application.WeatherRecommendationService;

import java.time.Clock;
import java.util.Objects;

public class OperatorDashboardService {

    private final AttractionService attractions;
    private final IncidentService incidents;
    private final WeatherRecommendationService weatherRecommendations;
    private final MaintenanceWorkOrderRepository workOrders;
    private final FlowRecommendationService flowRecommendations;
    private final Clock clock;

    public OperatorDashboardService(
            AttractionService attractions,
            IncidentService incidents,
            WeatherRecommendationService weatherRecommendations,
            MaintenanceWorkOrderRepository workOrders,
            FlowRecommendationService flowRecommendations,
            Clock clock
    ) {
        this.attractions = Objects.requireNonNull(attractions);
        this.incidents = Objects.requireNonNull(incidents);
        this.weatherRecommendations = Objects.requireNonNull(weatherRecommendations);
        this.workOrders = Objects.requireNonNull(workOrders);
        this.flowRecommendations = Objects.requireNonNull(flowRecommendations);
        this.clock = Objects.requireNonNull(clock);
    }

    public OperatorDashboardResponse snapshot() {
        return OperatorDashboardAssembler.assemble(
                clock.instant(),
                attractions.list(),
                incidents.list(),
                weatherRecommendations.list(),
                workOrders.findAll(),
                flowRecommendations.list()
        );
    }
}
