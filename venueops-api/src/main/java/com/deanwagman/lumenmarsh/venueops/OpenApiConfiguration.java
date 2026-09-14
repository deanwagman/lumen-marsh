package com.deanwagman.lumenmarsh.venueops;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfiguration {

    @Bean
    OpenAPI venueOpsOpenApi() {
        return new OpenAPI().info(new Info()
                .title("VenueOps API")
                .version("v1")
                .description("Operational core for Lumen Marsh attractions, incidents, and weather recommendations. Guest clients subscribe to GET /api/v1/attractions/events or GET /api/v1/events. Control Tower subscribes to GET /api/v1/operator/events. Reconnection receives fresh snapshots; missed events are not replayed."));
    }
}
