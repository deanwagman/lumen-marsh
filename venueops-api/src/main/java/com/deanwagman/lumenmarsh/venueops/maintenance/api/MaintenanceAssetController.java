package com.deanwagman.lumenmarsh.venueops.maintenance.api;

import com.deanwagman.lumenmarsh.venueops.maintenance.application.MaintenanceAssetFilter;
import com.deanwagman.lumenmarsh.venueops.maintenance.application.MaintenanceAssetQueryService;
import com.deanwagman.lumenmarsh.venueops.maintenance.application.MaintenanceWorkOrderFilter;
import com.deanwagman.lumenmarsh.venueops.maintenance.application.PageResult;
import com.deanwagman.lumenmarsh.venueops.maintenance.domain.asset.AssetCriticality;
import com.deanwagman.lumenmarsh.venueops.maintenance.domain.asset.AssetServiceStatus;
import com.deanwagman.lumenmarsh.venueops.maintenance.domain.asset.AssetType;
import com.deanwagman.lumenmarsh.venueops.maintenance.domain.asset.MaintenanceAsset;
import com.deanwagman.lumenmarsh.venueops.maintenance.domain.asset.MaintenanceAssetId;
import com.deanwagman.lumenmarsh.venueops.maintenance.domain.workorder.MaintenanceWorkOrder;
import com.deanwagman.lumenmarsh.venueops.security.VenueOpsScopes;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/operator/maintenance/assets")
@Tag(name = "Operator maintenance assets")
public class MaintenanceAssetController {

    private final MaintenanceAssetQueryService assets;

    public MaintenanceAssetController(MaintenanceAssetQueryService assets) {
        this.assets = assets;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('" + VenueOpsScopes.SCOPE_MAINTENANCE_READ + "')")
    @Operation(summary = "List maintenance assets")
    public PageResponse<MaintenanceAssetResponse> list(
            @RequestParam(required = false) String attractionId,
            @RequestParam(required = false) AssetType assetType,
            @RequestParam(required = false) AssetCriticality criticality,
            @RequestParam(required = false) AssetServiceStatus serviceStatus,
            @RequestParam(required = false) UUID parentAssetId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size
    ) {
        PageResult<MaintenanceAsset> result = assets.list(new MaintenanceAssetFilter(
                attractionId,
                assetType,
                criticality,
                serviceStatus,
                parentAssetId == null ? null : new MaintenanceAssetId(parentAssetId),
                page,
                size
        ));
        return new PageResponse<>(
                result.items().stream().map(MaintenanceAssetResponse::from).toList(),
                result.page(),
                result.size(),
                result.total()
        );
    }

    @GetMapping("/{assetId}")
    @PreAuthorize("hasAuthority('" + VenueOpsScopes.SCOPE_MAINTENANCE_READ + "')")
    @Operation(summary = "Get maintenance asset detail")
    public MaintenanceAssetResponse get(@PathVariable UUID assetId) {
        return MaintenanceAssetResponse.from(assets.get(new MaintenanceAssetId(assetId)));
    }

    @GetMapping("/{assetId}/work-orders")
    @PreAuthorize("hasAuthority('" + VenueOpsScopes.SCOPE_MAINTENANCE_READ + "')")
    @Operation(summary = "List work orders for an asset")
    public PageResponse<OperatorWorkOrderResponse> workOrders(
            @PathVariable UUID assetId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size
    ) {
        PageResult<MaintenanceWorkOrder> result = assets.workOrders(
                new MaintenanceAssetId(assetId),
                new MaintenanceWorkOrderFilter(
                        null, null, null, null, null, null, null, null, null, null, page, size
                )
        );
        return new PageResponse<>(
                result.items().stream()
                        .map(workOrder -> OperatorWorkOrderResponse.from(
                                workOrder,
                                assets.get(workOrder.assetId()),
                                null
                        ))
                        .toList(),
                result.page(),
                result.size(),
                result.total()
        );
    }
}
