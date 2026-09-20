package com.deanwagman.lumenmarsh.venueops.flow.api;

import com.deanwagman.lumenmarsh.venueops.attraction.domain.AttractionId;
import com.deanwagman.lumenmarsh.venueops.flow.application.FlowObservationIngestResult;
import com.deanwagman.lumenmarsh.venueops.flow.application.FlowObservationService;
import com.deanwagman.lumenmarsh.venueops.flow.domain.QueueObservationSourceType;
import com.deanwagman.lumenmarsh.venueops.security.ActorAuditContext;
import com.deanwagman.lumenmarsh.venueops.security.ActorIdentity;
import com.deanwagman.lumenmarsh.venueops.security.ActorResolver;
import com.deanwagman.lumenmarsh.venueops.security.VenueOpsScopes;
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

@RestController
@RequestMapping("/api/v1/integrations/flow/observations")
@Tag(name = "Park flow observation ingest")
public class FlowObservationIntegrationController {

    private final FlowObservationService observations;
    private final ActorResolver actorResolver;

    public FlowObservationIntegrationController(
            FlowObservationService observations,
            ActorResolver actorResolver
    ) {
        this.observations = observations;
        this.actorResolver = actorResolver;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('" + VenueOpsScopes.SCOPE_FLOW_INGEST_WRITE + "')")
    @Operation(summary = "Ingest a queue observation", description = "Idempotent on observationId. Out-of-order observations are stored but do not replace a newer projection.")
    public ResponseEntity<IngestFlowObservationResponse> ingest(@Valid @RequestBody IngestFlowObservationRequest request) {
        ActorIdentity actor = actorResolver.requireActor();
        FlowObservationIngestResult result = ActorAuditContext.call(actor, () -> observations.ingest(
                request.observationId(),
                new AttractionId(request.attractionId()),
                request.observedAt(),
                request.windowSeconds(),
                request.queueLength(),
                request.arrivals(),
                request.boarded(),
                request.operatingUnits(),
                request.configuredUnits(),
                request.sourceType() == null ? QueueObservationSourceType.SIMULATOR : request.sourceType(),
                Boolean.TRUE.equals(request.simulated())
        ));
        HttpStatus status = result.replay() ? HttpStatus.OK : HttpStatus.CREATED;
        return ResponseEntity.status(status).body(new IngestFlowObservationResponse(
                result.observationId(),
                result.accepted(),
                result.replay(),
                result.projectionVersion()
        ));
    }
}
