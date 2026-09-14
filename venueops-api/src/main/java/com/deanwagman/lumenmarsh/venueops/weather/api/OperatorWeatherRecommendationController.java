package com.deanwagman.lumenmarsh.venueops.weather.api;

import com.deanwagman.lumenmarsh.venueops.security.ActorAuditContext;
import com.deanwagman.lumenmarsh.venueops.security.ActorIdentity;
import com.deanwagman.lumenmarsh.venueops.security.ActorResolver;
import com.deanwagman.lumenmarsh.venueops.security.CommandAuthorization;
import com.deanwagman.lumenmarsh.venueops.security.VenueOpsScopes;
import com.deanwagman.lumenmarsh.venueops.weather.application.WeatherRecommendationService;
import com.deanwagman.lumenmarsh.venueops.weather.domain.WeatherRecommendationId;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/operator/weather/recommendations")
@Tag(name = "Operator weather recommendations")
public class OperatorWeatherRecommendationController {

    private final WeatherRecommendationService weatherRecommendationService;
    private final ActorResolver actorResolver;
    private final CommandAuthorization commandAuthorization;

    public OperatorWeatherRecommendationController(
            WeatherRecommendationService weatherRecommendationService,
            ActorResolver actorResolver,
            CommandAuthorization commandAuthorization
    ) {
        this.weatherRecommendationService = weatherRecommendationService;
        this.actorResolver = actorResolver;
        this.commandAuthorization = commandAuthorization;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('" + VenueOpsScopes.SCOPE_WEATHER_REVIEW + "')")
    @Operation(summary = "List weather recommendations")
    public List<OperatorWeatherRecommendationResponse> list() {
        return weatherRecommendationService.list().stream()
                .map(OperatorWeatherRecommendationResponse::from)
                .toList();
    }

    @GetMapping("/{recommendationId}")
    @PreAuthorize("hasAuthority('" + VenueOpsScopes.SCOPE_WEATHER_REVIEW + "')")
    @Operation(summary = "Get weather recommendation detail")
    public OperatorWeatherRecommendationResponse get(@PathVariable String recommendationId) {
        return OperatorWeatherRecommendationResponse.from(
                weatherRecommendationService.get(new WeatherRecommendationId(recommendationId))
        );
    }

    @PostMapping("/{recommendationId}/commands")
    @PreAuthorize("hasAuthority('" + VenueOpsScopes.SCOPE_WEATHER_REVIEW + "')")
    @Operation(
            summary = "Execute a weather-recommendation command",
            description = """
                    ACKNOWLEDGE and DISMISS take an optional reason. LINK_INCIDENT requires incidentId of an existing \
                    incident. Attraction commands are not available here. Every command requires expectedVersion."""
    )
    public OperatorWeatherRecommendationResponse command(
            @PathVariable String recommendationId,
            @Valid @RequestBody WeatherRecommendationCommandRequest request
    ) {
        commandAuthorization.requireWeatherReviewCommand();
        ActorIdentity actor = actorResolver.requireActor();
        return ActorAuditContext.call(actor, () -> OperatorWeatherRecommendationResponse.from(
                weatherRecommendationService.execute(
                        new WeatherRecommendationId(recommendationId),
                        request.type(),
                        actor.auditLabel(),
                        request.reason(),
                        request.expectedVersion(),
                        request.incidentId()
                )
        ));
    }
}
