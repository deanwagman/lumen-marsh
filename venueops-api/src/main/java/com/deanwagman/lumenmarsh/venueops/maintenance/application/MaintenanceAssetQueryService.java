package com.deanwagman.lumenmarsh.venueops.maintenance.application;

import com.deanwagman.lumenmarsh.venueops.maintenance.domain.asset.MaintenanceAsset;
import com.deanwagman.lumenmarsh.venueops.maintenance.domain.asset.MaintenanceAssetId;
import com.deanwagman.lumenmarsh.venueops.maintenance.domain.workorder.MaintenanceWorkOrder;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public class MaintenanceAssetQueryService {

    private final MaintenanceAssetRepository assets;
    private final MaintenanceWorkOrderRepository workOrders;

    public MaintenanceAssetQueryService(
            MaintenanceAssetRepository assets,
            MaintenanceWorkOrderRepository workOrders
    ) {
        this.assets = Objects.requireNonNull(assets);
        this.workOrders = Objects.requireNonNull(workOrders);
    }

    public PageResult<MaintenanceAsset> list(MaintenanceAssetFilter filter) {
        List<MaintenanceAsset> matches = assets.findAll().stream()
                .filter(asset -> matches(asset, filter))
                .sorted(Comparator.comparing(MaintenanceAsset::assetCode))
                .toList();
        return page(matches, filter.page(), filter.size());
    }

    public MaintenanceAsset get(MaintenanceAssetId id) {
        return assets.findById(id).orElseThrow(() -> new MaintenanceAssetNotFoundException(id));
    }

    public PageResult<MaintenanceWorkOrder> workOrders(MaintenanceAssetId assetId, MaintenanceWorkOrderFilter filter) {
        get(assetId);
        List<MaintenanceWorkOrder> matches = workOrders.findByAssetId(assetId).stream()
                .sorted(MaintenanceWorkOrderQueryService.workOrderOrder())
                .toList();
        return page(matches, filter.page(), filter.size());
    }

    private static boolean matches(MaintenanceAsset asset, MaintenanceAssetFilter filter) {
        if (filter.attractionId() != null && !filter.attractionId().isBlank()
                && !asset.attractionId().value().equals(filter.attractionId())) {
            return false;
        }
        if (filter.assetType() != null && asset.assetType() != filter.assetType()) {
            return false;
        }
        if (filter.criticality() != null && asset.criticality() != filter.criticality()) {
            return false;
        }
        if (filter.serviceStatus() != null && asset.serviceStatus() != filter.serviceStatus()) {
            return false;
        }
        if (filter.parentAssetId() != null) {
            return filter.parentAssetId().equals(asset.parentAssetId());
        }
        return true;
    }

    static <T> PageResult<T> page(List<T> items, int page, int size) {
        int from = Math.min(page * size, items.size());
        int to = Math.min(from + size, items.size());
        return new PageResult<>(items.subList(from, to), page, size, items.size());
    }
}
