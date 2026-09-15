package com.deanwagman.lumenmarsh.venueops.maintenance.api;

import com.deanwagman.lumenmarsh.venueops.maintenance.application.MaintenanceRecommendationService;
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
@RequestMapping("/api/v1/integrations/reliability/recommendations")
@Tag(name = "Reliability recommendation ingest")
public class ReliabilityRecommendationIntegrationController {

    private final MaintenanceRecommendationService recommendations;
    private final ActorResolver actorResolver;

    public ReliabilityRecommendationIntegrationController(
            MaintenanceRecommendationService recommendations,
            ActorResolver actorResolver
    ) {
        this.recommendations = recommendations;
        this.actorResolver = actorResolver;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('" + VenueOpsScopes.SCOPE_RELIABILITY_WRITE + "')")
    @Operation(summary = "Ingest a reliability recommendation", description = "Creates a pending recommendation. Duplicate observation IDs are accepted without creating a work order.")
    public ResponseEntity<ReliabilityRecommendationIngestResponse> ingest(
            @Valid @RequestBody IngestReliabilityRecommendationRequest request
    ) {
        ActorIdentity actor = actorResolver.requireActor();
        MaintenanceRecommendationService.RecommendationIngestResult result = ActorAuditContext.call(
                actor,
                () -> recommendations.ingest(
                        request.observationId(),
                        request.observedAt(),
                        request.assetCode(),
                        request.signalType(),
                        request.severity(),
                        request.value(),
                        request.unit(),
                        request.evidence(),
                        request.recommendedAction()
                )
        );
        HttpStatus status = result.duplicate() ? HttpStatus.OK : HttpStatus.CREATED;
        return ResponseEntity.status(status).body(ReliabilityRecommendationIngestResponse.from(result));
    }
}
