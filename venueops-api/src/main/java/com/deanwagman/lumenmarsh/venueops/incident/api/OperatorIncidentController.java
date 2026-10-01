package com.deanwagman.lumenmarsh.venueops.incident.api;

import com.deanwagman.lumenmarsh.venueops.incident.application.IncidentService;
import com.deanwagman.lumenmarsh.venueops.incident.application.RelatedMaintenanceWorkOrders;
import com.deanwagman.lumenmarsh.venueops.incident.domain.Incident;
import com.deanwagman.lumenmarsh.venueops.incident.domain.IncidentId;
import com.deanwagman.lumenmarsh.venueops.security.ActorAuditContext;
import com.deanwagman.lumenmarsh.venueops.security.ActorIdentity;
import com.deanwagman.lumenmarsh.venueops.security.ActorResolver;
import com.deanwagman.lumenmarsh.venueops.security.CommandAuthorization;
import com.deanwagman.lumenmarsh.venueops.security.VenueOpsScopes;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/operator/incidents")
@Tag(name = "Operator incidents")
public class OperatorIncidentController {

    private final IncidentService incidentService;
    private final ActorResolver actorResolver;
    private final CommandAuthorization commandAuthorization;
    private final RelatedMaintenanceWorkOrders relatedMaintenanceWorkOrders;

    public OperatorIncidentController(
            IncidentService incidentService,
            ActorResolver actorResolver,
            CommandAuthorization commandAuthorization,
            RelatedMaintenanceWorkOrders relatedMaintenanceWorkOrders
    ) {
        this.incidentService = incidentService;
        this.actorResolver = actorResolver;
        this.commandAuthorization = commandAuthorization;
        this.relatedMaintenanceWorkOrders = relatedMaintenanceWorkOrders;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('" + VenueOpsScopes.SCOPE_INCIDENTS_COMMAND + "')")
    @Operation(summary = "Report an incident", description = "Creates an incident at version 1 with an IncidentReported activity. Linking attractions does not change attraction state.")
    public OperatorIncidentResponse report(@Valid @RequestBody ReportIncidentRequest request) {
        ActorIdentity actor = actorResolver.requireActor();
        return ActorAuditContext.call(actor, () -> OperatorIncidentResponse.from(incidentService.report(
                request.title(),
                request.type(),
                request.severity(),
                request.internalDescription(),
                request.attractionIds(),
                actor.auditLabel()
        )));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('" + VenueOpsScopes.SCOPE_OPERATOR_READ + "')")
    @Operation(summary = "List incidents")
    public List<OperatorIncidentResponse> list() {
        return incidentService.list().stream().map(OperatorIncidentResponse::from).toList();
    }

    @GetMapping("/{incidentId}")
    @PreAuthorize("hasAuthority('" + VenueOpsScopes.SCOPE_OPERATOR_READ + "')")
    @Operation(summary = "Get incident detail")
    public OperatorIncidentResponse get(@PathVariable String incidentId) {
        Incident incident = incidentService.get(new IncidentId(incidentId));
        return OperatorIncidentResponse.from(incident, relatedMaintenanceWorkOrders.forIncident(incident.id()));
    }

    @GetMapping("/{incidentId}/activity")
    @PreAuthorize("hasAuthority('" + VenueOpsScopes.SCOPE_OPERATOR_READ + "')")
    @Operation(summary = "Get incident activity history")
    public List<IncidentActivityResponse> activity(@PathVariable String incidentId) {
        return incidentService.activity(new IncidentId(incidentId)).stream()
                .map(IncidentActivityResponse::from)
                .toList();
    }

    @PostMapping("/{incidentId}/commands")
    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary = "Execute an incident command",
            description = """
                    ACKNOWLEDGE: optional reason. ASSIGN: assignee. START_MITIGATION: optional reason. \
                    CHANGE_SEVERITY: severity and required reason. LINK_ATTRACTION: attractionId. \
                    UNLINK_ATTRACTION: attractionId and required reason. PUBLISH_GUEST_ADVISORY: guestTitle and guestMessage. \
                    WITHDRAW_GUEST_ADVISORY: required reason. RESOLVE: required reason. \
                    Every command requires commandId and expectedVersion. Extra fields belong in data."""
    )
    public OperatorIncidentResponse command(
            @PathVariable String incidentId,
            @Valid @RequestBody IncidentCommandRequest request
    ) {
        Incident incident = incidentService.get(new IncidentId(incidentId));
        commandAuthorization.requireIncidentCommand(request.type(), incident.severity());
        ActorIdentity actor = actorResolver.requireActor();
        return ActorAuditContext.call(actor, () -> OperatorIncidentResponse.from(incidentService.execute(
                new IncidentId(incidentId),
                request.commandId(),
                request.type(),
                actor.auditLabel(),
                request.reason(),
                request.expectedVersion(),
                request.assignee(),
                request.severity(),
                request.attractionId(),
                request.guestTitle(),
                request.guestMessage(),
                request.confirmActiveWorkOrders()
        )));
    }
}
