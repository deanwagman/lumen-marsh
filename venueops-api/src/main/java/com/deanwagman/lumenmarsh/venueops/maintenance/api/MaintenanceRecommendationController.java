package com.deanwagman.lumenmarsh.venueops.maintenance.api;

import com.deanwagman.lumenmarsh.venueops.maintenance.application.MaintenanceRecommendationService;
import com.deanwagman.lumenmarsh.venueops.maintenance.domain.recommendation.MaintenanceRecommendationId;
import com.deanwagman.lumenmarsh.venueops.security.ActorAuditContext;
import com.deanwagman.lumenmarsh.venueops.security.ActorIdentity;
import com.deanwagman.lumenmarsh.venueops.security.ActorResolver;
import com.deanwagman.lumenmarsh.venueops.security.VenueOpsScopes;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/operator/maintenance/recommendations")
@Tag(name = "Operator reliability recommendations")
public class MaintenanceRecommendationController {

    private final MaintenanceRecommendationService recommendations;
    private final ActorResolver actorResolver;

    public MaintenanceRecommendationController(
            MaintenanceRecommendationService recommendations,
            ActorResolver actorResolver
    ) {
        this.recommendations = recommendations;
        this.actorResolver = actorResolver;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('" + VenueOpsScopes.SCOPE_MAINTENANCE_READ + "')")
    @Operation(summary = "List reliability recommendations")
    public List<OperatorReliabilityRecommendationResponse> list() {
        return recommendations.list().stream()
                .map(OperatorReliabilityRecommendationResponse::from)
                .toList();
    }

    @GetMapping("/{recommendationId}")
    @PreAuthorize("hasAuthority('" + VenueOpsScopes.SCOPE_MAINTENANCE_READ + "')")
    @Operation(summary = "Get a reliability recommendation")
    public OperatorReliabilityRecommendationResponse get(@PathVariable UUID recommendationId) {
        return OperatorReliabilityRecommendationResponse.from(
                recommendations.get(new MaintenanceRecommendationId(recommendationId))
        );
    }

    @PostMapping("/{recommendationId}/commands")
    @PreAuthorize("hasAuthority('" + VenueOpsScopes.SCOPE_MAINTENANCE_COMMAND + "')")
    @Operation(summary = "Accept or dismiss a reliability recommendation. Every command requires commandId and expectedVersion.")
    public OperatorReliabilityRecommendationResponse command(
            @PathVariable UUID recommendationId,
            @Valid @RequestBody MaintenanceRecommendationCommandRequest request,
            @RequestHeader(value = "X-Correlation-Id", required = false) String correlationId
    ) {
        ActorIdentity actor = actorResolver.requireActor();
        String correlation = correlationId == null || correlationId.isBlank()
                ? UUID.randomUUID().toString()
                : correlationId.trim();
        return ActorAuditContext.call(actor, () -> OperatorReliabilityRecommendationResponse.from(
                recommendations.execute(
                        new MaintenanceRecommendationId(recommendationId),
                        request.commandId(),
                        request.expectedVersion(),
                        request.type(),
                        actor,
                        correlation
                )
        ));
    }
}
