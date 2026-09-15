package com.deanwagman.lumenmarsh.venueops.maintenance.application;

import com.deanwagman.lumenmarsh.venueops.maintenance.domain.asset.MaintenanceAsset;
import com.deanwagman.lumenmarsh.venueops.maintenance.domain.asset.MaintenanceAssetId;

import java.util.List;
import java.util.Optional;

public interface MaintenanceAssetRepository {

    Optional<MaintenanceAsset> findById(MaintenanceAssetId id);

    Optional<MaintenanceAsset> findByAssetCode(String assetCode);

    List<MaintenanceAsset> findAll();

    void save(MaintenanceAsset asset);
}
