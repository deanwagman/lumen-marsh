package com.deanwagman.lumenmarsh.venueops.maintenance.api;

import com.deanwagman.lumenmarsh.venueops.maintenance.domain.asset.AssetCriticality;
import com.deanwagman.lumenmarsh.venueops.maintenance.domain.asset.AssetServiceStatus;
import com.deanwagman.lumenmarsh.venueops.maintenance.domain.asset.AssetType;
import com.deanwagman.lumenmarsh.venueops.maintenance.domain.asset.MaintenanceAsset;

import java.time.Instant;
import java.util.UUID;

public record MaintenanceAssetResponse(
        UUID id,
        String assetCode,
        String name,
        AssetType assetType,
        String attractionId,
        UUID parentAssetId,
        AssetCriticality criticality,
        AssetServiceStatus serviceStatus,
        String manufacturer,
        String model,
        Instant installedAt,
        long version,
        Instant createdAt,
        Instant updatedAt
) {
    public static MaintenanceAssetResponse from(MaintenanceAsset asset) {
        return new MaintenanceAssetResponse(
                asset.id().value(),
                asset.assetCode(),
                asset.name(),
                asset.assetType(),
                asset.attractionId().value(),
                asset.parentAssetId() == null ? null : asset.parentAssetId().value(),
                asset.criticality(),
                asset.serviceStatus(),
                asset.manufacturer(),
                asset.model(),
                asset.installedAt(),
                asset.version(),
                asset.createdAt(),
                asset.updatedAt()
        );
    }
}
