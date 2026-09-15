package com.deanwagman.lumenmarsh.venueops.maintenance.application;

import com.deanwagman.lumenmarsh.venueops.maintenance.domain.asset.AssetCriticality;
import com.deanwagman.lumenmarsh.venueops.maintenance.domain.asset.AssetServiceStatus;
import com.deanwagman.lumenmarsh.venueops.maintenance.domain.asset.AssetType;
import com.deanwagman.lumenmarsh.venueops.maintenance.domain.asset.MaintenanceAssetId;

public record MaintenanceAssetFilter(
        String attractionId,
        AssetType assetType,
        AssetCriticality criticality,
        AssetServiceStatus serviceStatus,
        MaintenanceAssetId parentAssetId,
        int page,
        int size
) {
    public static final int DEFAULT_SIZE = 50;
    public static final int MAX_SIZE = 100;

    public MaintenanceAssetFilter {
        if (page < 0) {
            page = 0;
        }
        if (size <= 0) {
            size = DEFAULT_SIZE;
        }
        if (size > MAX_SIZE) {
            size = MAX_SIZE;
        }
    }
}
