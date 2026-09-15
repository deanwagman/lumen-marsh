package com.deanwagman.lumenmarsh.venueops.maintenance.api;

import com.deanwagman.lumenmarsh.venueops.incident.application.IncidentNotFoundException;
import com.deanwagman.lumenmarsh.venueops.incident.application.IncidentService;
import com.deanwagman.lumenmarsh.venueops.incident.domain.Incident;
import com.deanwagman.lumenmarsh.venueops.incident.domain.IncidentId;
import com.deanwagman.lumenmarsh.venueops.maintenance.application.MaintenanceAssetQueryService;
import com.deanwagman.lumenmarsh.venueops.maintenance.application.MaintenanceCommandResult;
import com.deanwagman.lumenmarsh.venueops.maintenance.application.MaintenanceWorkOrderCommandService;
import com.deanwagman.lumenmarsh.venueops.maintenance.application.MaintenanceWorkOrderFilter;
import com.deanwagman.lumenmarsh.venueops.maintenance.application.MaintenanceWorkOrderLifecycle;
import com.deanwagman.lumenmarsh.venueops.maintenance.application.MaintenanceWorkOrderQueryService;
import com.deanwagman.lumenmarsh.venueops.maintenance.application.PageResult;
import com.deanwagman.lumenmarsh.venueops.maintenance.domain.asset.MaintenanceAsset;
import com.deanwagman.lumenmarsh.venueops.maintenance.domain.asset.MaintenanceAssetId;
import com.deanwagman.lumenmarsh.venueops.maintenance.domain.workorder.MaintenanceClassification;
import com.deanwagman.lumenmarsh.venueops.maintenance.domain.workorder.MaintenancePriority;
import com.deanwagman.lumenmarsh.venueops.maintenance.domain.workorder.MaintenanceSourceType;
import com.deanwagman.lumenmarsh.venueops.maintenance.domain.workorder.MaintenanceWorkOrder;
import com.deanwagman.lumenmarsh.venueops.maintenance.domain.workorder.MaintenanceWorkOrderId;
import com.deanwagman.lumenmarsh.venueops.maintenance.domain.workorder.MaintenanceWorkOrderStatus;
import com.deanwagman.lumenmarsh.venueops.security.ActorAuditContext;
import com.deanwagman.lumenmarsh.venueops.security.ActorIdentity;
import com.deanwagman.lumenmarsh.venueops.security.ActorResolver;
import com.deanwagman.lumenmarsh.venueops.security.CommandAuthorization;
import com.deanwagman.lumenmarsh.venueops.security.VenueOpsScopes;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/operator/maintenance/work-orders")
@Tag(name = "Operator maintenance work orders")
public class MaintenanceWorkOrderController {

    private final MaintenanceWorkOrderQueryService workOrders;
    private final MaintenanceWorkOrderCommandService commands;
    private final MaintenanceAssetQueryService assets;
    private final IncidentService incidents;
    private final ActorResolver actorResolver;
    private final CommandAuthorization commandAuthorization;

    public MaintenanceWorkOrderController(
            MaintenanceWorkOrderQueryService workOrders,
            MaintenanceWorkOrderCommandService commands,
            MaintenanceAssetQueryService assets,
            IncidentService incidents,
            ActorResolver actorResolver,
            CommandAuthorization commandAuthorization
    ) {
        this.workOrders = workOrders;
        this.commands = commands;
        this.assets = assets;
        this.incidents = incidents;
        this.actorResolver = actorResolver;
        this.commandAuthorization = commandAuthorization;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('" + VenueOpsScopes.SCOPE_MAINTENANCE_READ + "')")
    @Operation(summary = "List maintenance work orders")
    public PageResponse<OperatorWorkOrderResponse> list(
            @RequestParam(required = false) MaintenanceWorkOrderStatus status,
            @RequestParam(required = false) MaintenancePriority priority,
            @RequestParam(required = false) MaintenanceClassification classification,
            @RequestParam(required = false) String attractionId,
            @RequestParam(required = false) UUID assetId,
            @RequestParam(required = false) String incidentId,
            @RequestParam(required = false) String assignedTeam,
            @RequestParam(required = false) Instant createdFrom,
            @RequestParam(required = false) Instant createdTo,
            @RequestParam(required = false) String lifecycle,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size
    ) {
        PageResult<MaintenanceWorkOrder> result = workOrders.list(new MaintenanceWorkOrderFilter(
                status,
                priority,
                classification,
                attractionId,
                assetId == null ? null : new MaintenanceAssetId(assetId),
                incidentId,
                assignedTeam,
                createdFrom,
                createdTo,
                MaintenanceWorkOrderLifecycle.fromQuery(lifecycle),
                page,
                size
        ));
        return new PageResponse<>(
                result.items().stream().map(this::toResponse).toList(),
                result.page(),
                result.size(),
                result.total()
        );
    }

    @PostMapping
    @PreAuthorize("hasAuthority('" + VenueOpsScopes.SCOPE_MAINTENANCE_COMMAND + "')")
    @Operation(summary = "Create a draft maintenance work order")
    public ResponseEntity<OperatorWorkOrderResponse> create(
            @Valid @RequestBody CreateWorkOrderRequest request,
            @RequestHeader(value = "X-Correlation-Id", required = false) String correlationId
    ) {
        ActorIdentity actor = actorResolver.requireActor();
        MaintenanceCommandResult result = ActorAuditContext.call(actor, () -> commands.create(
                request.commandId(),
                correlation(correlationId),
                new MaintenanceAssetId(request.assetId()),
                request.incidentId() == null ? null : new IncidentId(request.incidentId().toString()),
                request.sourceType() == null ? MaintenanceSourceType.MANUAL : request.sourceType(),
                request.sourceReferenceId(),
                request.classification(),
                request.priority(),
                request.summary(),
                request.description(),
                request.checklistDrafts(),
                actor
        ));
        OperatorWorkOrderResponse body = toResponse(result.workOrder());
        HttpStatus status = result.replay() ? HttpStatus.OK : HttpStatus.CREATED;
        return ResponseEntity.status(status)
                .location(URI.create("/api/v1/operator/maintenance/work-orders/" + body.id()))
                .body(body);
    }

    @GetMapping("/{workOrderId}")
    @PreAuthorize("hasAuthority('" + VenueOpsScopes.SCOPE_MAINTENANCE_READ + "')")
    @Operation(summary = "Get work-order detail")
    public OperatorWorkOrderResponse get(@PathVariable UUID workOrderId) {
        return toResponse(workOrders.get(new MaintenanceWorkOrderId(workOrderId)));
    }

    @GetMapping("/{workOrderId}/activity")
    @PreAuthorize("hasAuthority('" + VenueOpsScopes.SCOPE_MAINTENANCE_READ + "')")
    @Operation(summary = "Get work-order activity history")
    public java.util.List<MaintenanceActivityResponse> activity(@PathVariable UUID workOrderId) {
        return workOrders.activity(new MaintenanceWorkOrderId(workOrderId)).stream()
                .map(MaintenanceActivityResponse::from)
                .toList();
    }

    @PostMapping("/{workOrderId}/commands")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Execute a maintenance work-order command")
    public MaintenanceCommandResponse command(
            @PathVariable UUID workOrderId,
            @Valid @RequestBody MaintenanceWorkOrderCommandRequest request,
            @RequestHeader(value = "X-Correlation-Id", required = false) String correlationId
    ) {
        MaintenanceWorkOrder workOrder = workOrders.get(new MaintenanceWorkOrderId(workOrderId));
        commandAuthorization.requireMaintenanceCommand(request.type(), workOrder.priority());
        ActorIdentity actor = actorResolver.requireActor();
        boolean supervisor = commandAuthorization.isSupervisor();
        MaintenanceCommandResult result = ActorAuditContext.call(actor, () -> commands.execute(
                new MaintenanceWorkOrderId(workOrderId),
                request.commandId(),
                correlation(correlationId),
                request.type(),
                request.expectedVersion(),
                request.reason(),
                request.data(),
                actor,
                supervisor
        ));
        return MaintenanceCommandResponse.from(result);
    }

    private OperatorWorkOrderResponse toResponse(MaintenanceWorkOrder workOrder) {
        MaintenanceAsset asset = assets.get(workOrder.assetId());
        Incident incident = null;
        if (workOrder.incidentId() != null) {
            try {
                incident = incidents.get(workOrder.incidentId());
            } catch (IncidentNotFoundException ignored) {
                incident = null;
            }
        }
        return OperatorWorkOrderResponse.from(workOrder, asset, incident);
    }

    private static String correlation(String correlationId) {
        return correlationId == null || correlationId.isBlank() ? UUID.randomUUID().toString() : correlationId.trim();
    }
}
