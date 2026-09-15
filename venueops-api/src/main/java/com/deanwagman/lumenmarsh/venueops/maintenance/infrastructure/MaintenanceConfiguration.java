package com.deanwagman.lumenmarsh.venueops.maintenance.infrastructure;

import com.deanwagman.lumenmarsh.venueops.attraction.application.AttractionRepository;
import com.deanwagman.lumenmarsh.venueops.incident.application.IncidentService;
import com.deanwagman.lumenmarsh.venueops.maintenance.application.MaintenanceAssetQueryService;
import com.deanwagman.lumenmarsh.venueops.maintenance.application.MaintenanceAssetRepository;
import com.deanwagman.lumenmarsh.venueops.maintenance.application.MaintenanceMetrics;
import com.deanwagman.lumenmarsh.venueops.maintenance.application.MaintenanceRecommendationRepository;
import com.deanwagman.lumenmarsh.venueops.maintenance.application.MaintenanceRecommendationService;
import com.deanwagman.lumenmarsh.venueops.maintenance.application.MaintenanceUpdatePublisher;
import com.deanwagman.lumenmarsh.venueops.maintenance.application.MaintenanceWorkOrderCommandService;
import com.deanwagman.lumenmarsh.venueops.maintenance.application.MaintenanceWorkOrderQueryService;
import com.deanwagman.lumenmarsh.venueops.maintenance.application.MaintenanceWorkOrderRepository;
import com.deanwagman.lumenmarsh.venueops.maintenance.application.ProcessedCommandRepository;
import com.deanwagman.lumenmarsh.venueops.maintenance.application.WorkOrderNumberGenerator;
import com.deanwagman.lumenmarsh.venueops.maintenance.domain.asset.MaintenanceAsset;
import com.deanwagman.lumenmarsh.venueops.maintenance.domain.recommendation.MaintenanceRecommendation;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

import java.time.Clock;

@Configuration
public class MaintenanceConfiguration {

    @Bean
    MaintenanceAssetQueryService maintenanceAssetQueryService(
            MaintenanceAssetRepository assets,
            MaintenanceWorkOrderRepository workOrders
    ) {
        return new MaintenanceAssetQueryService(assets, workOrders);
    }

    @Bean
    MaintenanceWorkOrderQueryService maintenanceWorkOrderQueryService(MaintenanceWorkOrderRepository workOrders) {
        return new MaintenanceWorkOrderQueryService(workOrders);
    }

    @Bean
    MaintenanceMetrics maintenanceMetrics(
            MeterRegistry meterRegistry,
            MaintenanceWorkOrderRepository workOrders,
            MaintenanceRecommendationRepository recommendations
    ) {
        return new MaintenanceMetrics(meterRegistry, workOrders, recommendations);
    }

    @Bean
    MaintenanceWorkOrderCommandService maintenanceWorkOrderCommandService(
            MaintenanceWorkOrderRepository workOrders,
            MaintenanceAssetRepository assets,
            AttractionRepository attractions,
            IncidentService incidents,
            ProcessedCommandRepository processedCommands,
            WorkOrderNumberGenerator numbers,
            MaintenanceUpdatePublisher publisher,
            MaintenanceMetrics metrics,
            Clock clock
    ) {
        return new MaintenanceWorkOrderCommandService(
                workOrders,
                assets,
                attractions,
                incidents,
                processedCommands,
                numbers,
                publisher,
                metrics,
                clock
        );
    }

    @Bean
    MaintenanceRecommendationService maintenanceRecommendationService(
            MaintenanceRecommendationRepository recommendations,
            MaintenanceAssetRepository assets,
            MaintenanceWorkOrderCommandService workOrders,
            Clock clock
    ) {
        return new MaintenanceRecommendationService(recommendations, assets, workOrders, clock);
    }

    @Bean
    @Order(10)
    @ConditionalOnProperty(name = "venueops.attractions.seed", havingValue = "true")
    ApplicationRunner maintenanceSeedRunner(
            MaintenanceAssetRepository assets,
            MaintenanceRecommendationRepository recommendations,
            Clock clock
    ) {
        return args -> {
            if (assets.findAll().isEmpty()) {
                for (MaintenanceAsset asset : MaintenanceAssetSeedData.assets(clock)) {
                    assets.save(asset);
                }
            }
            if (recommendations.findAll().isEmpty()) {
                MaintenanceRecommendation seeded = MaintenanceRecommendationSeedData.cypressCoilVibration(clock);
                recommendations.save(seeded);
            }
        };
    }
}
