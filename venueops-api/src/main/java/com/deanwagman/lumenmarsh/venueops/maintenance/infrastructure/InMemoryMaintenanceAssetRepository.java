package com.deanwagman.lumenmarsh.venueops.maintenance.infrastructure;

import com.deanwagman.lumenmarsh.venueops.maintenance.application.MaintenanceAssetRepository;
import com.deanwagman.lumenmarsh.venueops.maintenance.domain.asset.MaintenanceAsset;
import com.deanwagman.lumenmarsh.venueops.maintenance.domain.asset.MaintenanceAssetId;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Repository
@ConditionalOnProperty(name = "venueops.attractions.persistence", havingValue = "memory", matchIfMissing = true)
public class InMemoryMaintenanceAssetRepository implements MaintenanceAssetRepository {

    private final Map<MaintenanceAssetId, MaintenanceAsset> assets = new ConcurrentHashMap<>();

    @Override
    public Optional<MaintenanceAsset> findById(MaintenanceAssetId id) {
        return Optional.ofNullable(assets.get(id));
    }

    @Override
    public Optional<MaintenanceAsset> findByAssetCode(String assetCode) {
        return assets.values().stream()
                .filter(asset -> asset.assetCode().equalsIgnoreCase(assetCode))
                .findFirst();
    }

    @Override
    public List<MaintenanceAsset> findAll() {
        return assets.values().stream()
                .sorted(Comparator.comparing(MaintenanceAsset::assetCode))
                .toList();
    }

    @Override
    public void save(MaintenanceAsset asset) {
        assets.put(asset.id(), asset);
    }
}
