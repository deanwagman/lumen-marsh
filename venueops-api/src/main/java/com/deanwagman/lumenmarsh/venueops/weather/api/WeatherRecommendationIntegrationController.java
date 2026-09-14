package com.deanwagman.lumenmarsh.venueops.weather.api;

import com.deanwagman.lumenmarsh.venueops.attraction.domain.AttractionId;
import com.deanwagman.lumenmarsh.venueops.security.ActorAuditContext;
import com.deanwagman.lumenmarsh.venueops.security.ActorIdentity;
import com.deanwagman.lumenmarsh.venueops.security.ActorResolver;
import com.deanwagman.lumenmarsh.venueops.security.VenueOpsScopes;
import com.deanwagman.lumenmarsh.venueops.weather.application.WeatherRecommendationIngestResult;
import com.deanwagman.lumenmarsh.venueops.weather.application.WeatherRecommendationService;
import com.deanwagman.lumenmarsh.venueops.weather.domain.WeatherRecommendationId;
import com.deanwagman.lumenmarsh.venueops.weather.domain.WeatherRecommendationInbound;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/integrations/weather/recommendations")
@Tag(name = "Weather recommendation ingest")
public class WeatherRecommendationIntegrationController {

    private final WeatherRecommendationService weatherRecommendationService;
    private final ActorResolver actorResolver;

    public WeatherRecommendationIntegrationController(
            WeatherRecommendationService weatherRecommendationService,
            ActorResolver actorResolver
    ) {
        this.weatherRecommendationService = weatherRecommendationService;
        this.actorResolver = actorResolver;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('" + VenueOpsScopes.SCOPE_WEATHER_WRITE + "')")
    @Operation(
            summary = "Ingest a weather recommendation",
            description = """
                    Idempotent inbox for Environmental Monitor. New recommendation IDs create a record. \
                    A newer source version updates the existing record. The same source version is accepted as a \
                    duplicate with no new activity. An older source version is rejected as stale."""
    )
    public ResponseEntity<OperatorWeatherRecommendationResponse> ingest(
            @Valid @RequestBody IngestWeatherRecommendationRequest request
    ) {
        ActorIdentity actor = actorResolver.requireActor();
        WeatherRecommendationIngestResult result = ActorAuditContext.call(
                actor,
                () -> weatherRecommendationService.ingest(toInbound(request))
        );
        HttpStatus status = result.created() ? HttpStatus.CREATED : HttpStatus.OK;
        return ResponseEntity.status(status).body(OperatorWeatherRecommendationResponse.from(result.recommendation()));
    }

    static WeatherRecommendationInbound toInbound(IngestWeatherRecommendationRequest request) {
        List<AttractionId> attractionIds = request.affectedAttractionIds() == null
                ? List.of()
                : request.affectedAttractionIds().stream().map(AttractionId::new).toList();
        return new WeatherRecommendationInbound(
                new WeatherRecommendationId(request.recommendationId()),
                request.ruleId(),
                request.status(),
                request.severity(),
                request.summary(),
                request.evidence(),
                request.recommendedAction(),
                attractionIds,
                request.observedAt(),
                request.version()
        );
    }
}
