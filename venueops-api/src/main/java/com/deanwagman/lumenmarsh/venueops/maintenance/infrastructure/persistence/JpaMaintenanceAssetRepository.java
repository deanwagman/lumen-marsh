package com.deanwagman.lumenmarsh.venueops.maintenance.infrastructure.persistence;

import com.deanwagman.lumenmarsh.venueops.attraction.domain.AttractionId;
import com.deanwagman.lumenmarsh.venueops.maintenance.application.MaintenanceAssetRepository;
import com.deanwagman.lumenmarsh.venueops.maintenance.domain.asset.MaintenanceAsset;
import com.deanwagman.lumenmarsh.venueops.maintenance.domain.asset.MaintenanceAssetId;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

interface MaintenanceAssetJpaRepository extends JpaRepository<MaintenanceAssetEntity, String> {
    Optional<MaintenanceAssetEntity> findByAssetCodeIgnoreCase(String assetCode);
}

@Repository
@ConditionalOnProperty(name = "venueops.attractions.persistence", havingValue = "jpa")
public class JpaMaintenanceAssetRepository implements MaintenanceAssetRepository {

    private final MaintenanceAssetJpaRepository assets;

    public JpaMaintenanceAssetRepository(MaintenanceAssetJpaRepository assets) {
        this.assets = assets;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<MaintenanceAsset> findById(MaintenanceAssetId id) {
        return assets.findById(id.toString()).map(JpaMaintenanceAssetRepository::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<MaintenanceAsset> findByAssetCode(String assetCode) {
        return assets.findByAssetCodeIgnoreCase(assetCode).map(JpaMaintenanceAssetRepository::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MaintenanceAsset> findAll() {
        return assets.findAll().stream()
                .map(JpaMaintenanceAssetRepository::toDomain)
                .sorted(Comparator.comparing(MaintenanceAsset::assetCode))
                .toList();
    }

    @Override
    @Transactional
    public void save(MaintenanceAsset asset) {
        assets.save(toEntity(asset));
    }

    private static MaintenanceAsset toDomain(MaintenanceAssetEntity entity) {
        return new MaintenanceAsset(
                MaintenanceAssetId.of(entity.getId()),
                entity.getAssetCode(),
                entity.getName(),
                entity.getAssetType(),
                new AttractionId(entity.getAttractionId()),
                entity.getParentAssetId() == null ? null : MaintenanceAssetId.of(entity.getParentAssetId()),
                entity.getCriticality(),
                entity.getServiceStatus(),
                entity.getManufacturer(),
                entity.getModel(),
                entity.getInstalledAt(),
                entity.getVersion(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    private static MaintenanceAssetEntity toEntity(MaintenanceAsset asset) {
        return new MaintenanceAssetEntity(
                asset.id().toString(),
                asset.assetCode(),
                asset.name(),
                asset.assetType(),
                asset.attractionId().value(),
                asset.parentAssetId() == null ? null : asset.parentAssetId().toString(),
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
