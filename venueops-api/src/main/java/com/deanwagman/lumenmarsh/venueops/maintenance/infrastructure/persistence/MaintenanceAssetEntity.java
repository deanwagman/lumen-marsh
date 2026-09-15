package com.deanwagman.lumenmarsh.venueops.maintenance.infrastructure.persistence;

import com.deanwagman.lumenmarsh.venueops.maintenance.domain.asset.AssetCriticality;
import com.deanwagman.lumenmarsh.venueops.maintenance.domain.asset.AssetServiceStatus;
import com.deanwagman.lumenmarsh.venueops.maintenance.domain.asset.AssetType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "maintenance_assets")
public class MaintenanceAssetEntity {

    @Id
    @Column(length = 36)
    private String id;

    @Column(name = "asset_code", nullable = false, unique = true, length = 64)
    private String assetCode;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "asset_type", nullable = false, length = 32)
    private AssetType assetType;

    @Column(name = "attraction_id", nullable = false, length = 64)
    private String attractionId;

    @Column(name = "parent_asset_id", length = 36)
    private String parentAssetId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private AssetCriticality criticality;

    @Enumerated(EnumType.STRING)
    @Column(name = "service_status", nullable = false, length = 32)
    private AssetServiceStatus serviceStatus;

    private String manufacturer;
    private String model;

    @Column(name = "installed_at")
    private Instant installedAt;

    @Column(nullable = false)
    private long version;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected MaintenanceAssetEntity() {
    }

    public MaintenanceAssetEntity(
            String id,
            String assetCode,
            String name,
            AssetType assetType,
            String attractionId,
            String parentAssetId,
            AssetCriticality criticality,
            AssetServiceStatus serviceStatus,
            String manufacturer,
            String model,
            Instant installedAt,
            long version,
            Instant createdAt,
            Instant updatedAt
    ) {
        this.id = id;
        this.assetCode = assetCode;
        this.name = name;
        this.assetType = assetType;
        this.attractionId = attractionId;
        this.parentAssetId = parentAssetId;
        this.criticality = criticality;
        this.serviceStatus = serviceStatus;
        this.manufacturer = manufacturer;
        this.model = model;
        this.installedAt = installedAt;
        this.version = version;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public String getId() {
        return id;
    }

    public String getAssetCode() {
        return assetCode;
    }

    public String getName() {
        return name;
    }

    public AssetType getAssetType() {
        return assetType;
    }

    public String getAttractionId() {
        return attractionId;
    }

    public String getParentAssetId() {
        return parentAssetId;
    }

    public AssetCriticality getCriticality() {
        return criticality;
    }

    public AssetServiceStatus getServiceStatus() {
        return serviceStatus;
    }

    public String getManufacturer() {
        return manufacturer;
    }

    public String getModel() {
        return model;
    }

    public Instant getInstalledAt() {
        return installedAt;
    }

    public long getVersion() {
        return version;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
