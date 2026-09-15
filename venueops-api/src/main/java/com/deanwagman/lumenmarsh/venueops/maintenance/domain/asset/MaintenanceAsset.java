package com.deanwagman.lumenmarsh.venueops.maintenance.domain.asset;

import com.deanwagman.lumenmarsh.venueops.attraction.domain.AttractionId;

import java.time.Instant;
import java.util.Objects;

public final class MaintenanceAsset {

    private final MaintenanceAssetId id;
    private final String assetCode;
    private final String name;
    private final AssetType assetType;
    private final AttractionId attractionId;
    private final MaintenanceAssetId parentAssetId;
    private final AssetCriticality criticality;
    private final AssetServiceStatus serviceStatus;
    private final String manufacturer;
    private final String model;
    private final Instant installedAt;
    private final long version;
    private final Instant createdAt;
    private final Instant updatedAt;

    public MaintenanceAsset(
            MaintenanceAssetId id,
            String assetCode,
            String name,
            AssetType assetType,
            AttractionId attractionId,
            MaintenanceAssetId parentAssetId,
            AssetCriticality criticality,
            AssetServiceStatus serviceStatus,
            String manufacturer,
            String model,
            Instant installedAt,
            long version,
            Instant createdAt,
            Instant updatedAt
    ) {
        this.id = Objects.requireNonNull(id, "id is required");
        this.assetCode = requireText(assetCode, "assetCode");
        this.name = requireText(name, "name");
        this.assetType = Objects.requireNonNull(assetType, "assetType is required");
        this.attractionId = Objects.requireNonNull(attractionId, "attractionId is required");
        this.parentAssetId = parentAssetId;
        this.criticality = Objects.requireNonNull(criticality, "criticality is required");
        this.serviceStatus = Objects.requireNonNull(serviceStatus, "serviceStatus is required");
        this.manufacturer = normalizeOptional(manufacturer);
        this.model = normalizeOptional(model);
        this.installedAt = installedAt;
        this.version = version;
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt is required");
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt is required");
    }

    public MaintenanceAssetId id() {
        return id;
    }

    public String assetCode() {
        return assetCode;
    }

    public String name() {
        return name;
    }

    public AssetType assetType() {
        return assetType;
    }

    public AttractionId attractionId() {
        return attractionId;
    }

    public MaintenanceAssetId parentAssetId() {
        return parentAssetId;
    }

    public AssetCriticality criticality() {
        return criticality;
    }

    public AssetServiceStatus serviceStatus() {
        return serviceStatus;
    }

    public String manufacturer() {
        return manufacturer;
    }

    public String model() {
        return model;
    }

    public Instant installedAt() {
        return installedAt;
    }

    public long version() {
        return version;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant updatedAt() {
        return updatedAt;
    }

    public boolean belongsTo(AttractionId attractionId) {
        return this.attractionId.equals(attractionId);
    }

    private static String normalizeOptional(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static String requireText(String value, String field) {
        Objects.requireNonNull(value, field + " is required");
        if (value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return value.trim();
    }
}
