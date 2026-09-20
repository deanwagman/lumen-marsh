package com.deanwagman.lumenmarsh.venueops.flow.api;

import com.deanwagman.lumenmarsh.venueops.attraction.domain.AttractionId;
import com.deanwagman.lumenmarsh.venueops.flow.application.FlowQueryService;
import com.deanwagman.lumenmarsh.venueops.flow.application.FlowRecommendationService;
import com.deanwagman.lumenmarsh.venueops.flow.application.PageResult;
import com.deanwagman.lumenmarsh.venueops.flow.domain.FlowRecommendation;
import com.deanwagman.lumenmarsh.venueops.flow.domain.FlowRecommendationCommand;
import com.deanwagman.lumenmarsh.venueops.flow.domain.FlowRecommendationId;
import com.deanwagman.lumenmarsh.venueops.flow.domain.FlowRecommendationSeverity;
import com.deanwagman.lumenmarsh.venueops.flow.domain.FlowRecommendationStatus;
import com.deanwagman.lumenmarsh.venueops.flow.domain.FlowRecommendationType;
import com.deanwagman.lumenmarsh.venueops.security.ActorAuditContext;
import com.deanwagman.lumenmarsh.venueops.security.ActorIdentity;
import com.deanwagman.lumenmarsh.venueops.security.ActorResolver;
import com.deanwagman.lumenmarsh.venueops.security.CommandAuthorization;
import com.deanwagman.lumenmarsh.venueops.security.VenueOpsScopes;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/operator/flow")
@Tag(name = "Operator park flow")
public class OperatorFlowController {

    private final FlowQueryService queries;
    private final FlowRecommendationService recommendations;
    private final ActorResolver actorResolver;
    private final CommandAuthorization commandAuthorization;

    public OperatorFlowController(
            FlowQueryService queries,
            FlowRecommendationService recommendations,
            ActorResolver actorResolver,
            CommandAuthorization commandAuthorization
    ) {
        this.queries = queries;
        this.recommendations = recommendations;
        this.actorResolver = actorResolver;
        this.commandAuthorization = commandAuthorization;
    }

    @GetMapping("/overview")
    @PreAuthorize("hasAuthority('" + VenueOpsScopes.SCOPE_FLOW_READ + "')")
    @Operation(summary = "Park flow overview")
    public FlowQueryService.OperatorOverview overview() {
        return queries.overview();
    }

    @GetMapping("/attractions/{attractionId}")
    @PreAuthorize("hasAuthority('" + VenueOpsScopes.SCOPE_FLOW_READ + "')")
    @Operation(summary = "Attraction queue and forecast detail")
    public FlowQueryService.OperatorAttractionDetail attraction(@PathVariable String attractionId) {
        return queries.attraction(new AttractionId(attractionId));
    }

    @GetMapping("/attractions/{attractionId}/history")
    @PreAuthorize("hasAuthority('" + VenueOpsScopes.SCOPE_FLOW_READ + "')")
    @Operation(summary = "Recent queue observations")
    public FlowQueryService.OperatorAttractionDetail history(@PathVariable String attractionId) {
        return queries.attraction(new AttractionId(attractionId));
    }

    @GetMapping("/recommendations")
    @PreAuthorize("hasAuthority('" + VenueOpsScopes.SCOPE_FLOW_READ + "')")
    @Operation(summary = "List flow recommendations")
    public PageResponse<OperatorFlowRecommendationResponse> recommendations(
            @RequestParam(required = false) FlowRecommendationStatus status,
            @RequestParam(required = false) FlowRecommendationSeverity severity,
            @RequestParam(required = false) FlowRecommendationType type,
            @RequestParam(required = false) String attractionId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        PageResult<FlowRecommendation> result = queries.recommendations(
                status,
                severity,
                type,
                attractionId == null ? null : new AttractionId(attractionId),
                page,
                size
        );
        return new PageResponse<>(
                result.items().stream().map(OperatorFlowRecommendationResponse::from).toList(),
                result.page(),
                result.size(),
                result.total()
        );
    }

    @GetMapping("/recommendations/{recommendationId}")
    @PreAuthorize("hasAuthority('" + VenueOpsScopes.SCOPE_FLOW_READ + "')")
    @Operation(summary = "Get a flow recommendation")
    public OperatorFlowRecommendationResponse recommendation(@PathVariable String recommendationId) {
        return OperatorFlowRecommendationResponse.from(queries.recommendation(new FlowRecommendationId(recommendationId)));
    }

    @GetMapping("/recommendations/{recommendationId}/activity")
    @PreAuthorize("hasAuthority('" + VenueOpsScopes.SCOPE_FLOW_READ + "')")
    @Operation(summary = "Recommendation audit activity")
    public List<FlowActivityResponse> activity(@PathVariable String recommendationId) {
        return queries.recommendation(new FlowRecommendationId(recommendationId)).activity().stream()
                .map(FlowActivityResponse::from)
                .toList();
    }

    @PostMapping("/recommendations/{recommendationId}/commands")
    @PreAuthorize("hasAuthority('" + VenueOpsScopes.SCOPE_FLOW_COMMAND + "') or hasAuthority('"
            + VenueOpsScopes.SCOPE_FLOW_PUBLISH + "')")
    @Operation(summary = "Approve, dismiss, publish, or withdraw a flow recommendation")
    public OperatorFlowRecommendationResponse command(
            @PathVariable String recommendationId,
            @Valid @RequestBody FlowRecommendationCommandRequest request,
            HttpServletRequest httpRequest
    ) {
        if (request.type() == FlowRecommendationCommand.PUBLISH || request.type() == FlowRecommendationCommand.WITHDRAW) {
            commandAuthorization.requireFlowPublish();
        } else {
            commandAuthorization.requireFlowCommand();
        }
        ActorIdentity actor = actorResolver.requireActor();
        String correlationId = httpRequest.getHeader("X-Correlation-Id");
        String guestMessage = guestMessage(request.data());
        return ActorAuditContext.call(actor, () -> OperatorFlowRecommendationResponse.from(
                recommendations.execute(
                        new FlowRecommendationId(recommendationId),
                        request.commandId(),
                        correlationId,
                        request.type(),
                        request.expectedVersion(),
                        request.reason(),
                        guestMessage,
                        actor
                ).recommendation()
        ));
    }

    private static String guestMessage(Map<String, Object> data) {
        if (data == null) {
            return null;
        }
        Object value = data.get("guestMessage");
        return value == null ? null : String.valueOf(value);
    }
}
