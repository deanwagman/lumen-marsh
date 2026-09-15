package com.deanwagman.lumenmarsh.venueops.maintenance.domain.asset;

import java.util.Objects;
import java.util.UUID;

public record MaintenanceAssetId(UUID value) {

    public MaintenanceAssetId {
        Objects.requireNonNull(value, "asset id is required");
    }

    public static MaintenanceAssetId of(String value) {
        Objects.requireNonNull(value, "asset id is required");
        return new MaintenanceAssetId(UUID.fromString(value));
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
