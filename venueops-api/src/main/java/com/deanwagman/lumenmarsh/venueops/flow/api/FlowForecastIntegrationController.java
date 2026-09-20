package com.deanwagman.lumenmarsh.venueops.flow.api;

import com.deanwagman.lumenmarsh.venueops.attraction.domain.AttractionId;
import com.deanwagman.lumenmarsh.venueops.flow.application.FlowForecastService;
import com.deanwagman.lumenmarsh.venueops.flow.domain.QueueForecast;
import com.deanwagman.lumenmarsh.venueops.security.ActorAuditContext;
import com.deanwagman.lumenmarsh.venueops.security.ActorIdentity;
import com.deanwagman.lumenmarsh.venueops.security.ActorResolver;
import com.deanwagman.lumenmarsh.venueops.security.VenueOpsScopes;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/integrations/flow/forecasts")
@Tag(name = "Park flow forecast ingest")
public class FlowForecastIntegrationController {

    private final FlowForecastService forecasts;
    private final ActorResolver actorResolver;

    public FlowForecastIntegrationController(FlowForecastService forecasts, ActorResolver actorResolver) {
        this.forecasts = forecasts;
        this.actorResolver = actorResolver;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('" + VenueOpsScopes.SCOPE_FLOW_INGEST_WRITE + "')")
    @Operation(summary = "Ingest forecasts and an optional recommendation proposal")
    public ResponseEntity<IngestFlowForecastResponse> ingest(
            @Valid @RequestBody IngestFlowForecastRequest request,
            HttpServletRequest httpRequest
    ) {
        ActorIdentity actor = actorResolver.requireActor();
        String correlationId = httpRequest.getHeader("X-Correlation-Id");
        FlowForecastService.ForecastIngestResult result = ActorAuditContext.call(actor, () -> forecasts.ingest(
                new AttractionId(request.attractionId()),
                request.generatedAt(),
                request.basedOnObservationId(),
                Boolean.TRUE.equals(request.simulated()),
                request.forecasts().stream()
                        .map(horizon -> new FlowForecastService.HorizonInbound(
                                horizon.forecastId(),
                                horizon.horizonMinutes(),
                                horizon.predictedQueueLength(),
                                horizon.predictedWaitMinutes(),
                                horizon.confidence(),
                                horizon.assumptions(),
                                horizon.explanation()
                        ))
                        .toList(),
                toProposal(request.recommendation()),
                actor,
                correlationId
        ));
        HttpStatus status = result.replay() ? HttpStatus.OK : HttpStatus.CREATED;
        return ResponseEntity.status(status).body(new IngestFlowForecastResponse(
                true,
                result.replay(),
                result.forecasts().stream().map(QueueForecast::forecastId).toList(),
                result.recommendationAccepted(),
                result.recommendation() == null ? null : result.recommendation().id().value(),
                result.recommendationSkipReason()
        ));
    }

    private static FlowForecastService.RecommendationProposal toProposal(
            IngestFlowForecastRequest.RecommendationProposal proposal
    ) {
        if (proposal == null) {
            return null;
        }
        return new FlowForecastService.RecommendationProposal(
                proposal.recommendationId(),
                proposal.type(),
                proposal.severity(),
                proposal.sourceAttractionId(),
                proposal.affectedAttractionIds(),
                proposal.recommendedDestinationIds(),
                proposal.summary(),
                proposal.explanation(),
                proposal.guestMessage(),
                proposal.expiresAt(),
                proposal.relatedIncidentId(),
                proposal.relatedWorkOrderId()
        );
    }
}
