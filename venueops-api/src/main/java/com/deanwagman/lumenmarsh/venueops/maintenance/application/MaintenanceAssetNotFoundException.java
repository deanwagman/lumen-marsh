package com.deanwagman.lumenmarsh.venueops.maintenance.application;

import com.deanwagman.lumenmarsh.venueops.maintenance.domain.asset.MaintenanceAssetId;

public class MaintenanceAssetNotFoundException extends RuntimeException {

    private final String identifier;

    public MaintenanceAssetNotFoundException(MaintenanceAssetId id) {
        super("Maintenance asset not found: " + id);
        this.identifier = id.toString();
    }

    public MaintenanceAssetNotFoundException(String assetCode) {
        super("Maintenance asset not found: " + assetCode);
        this.identifier = assetCode;
    }

    public String identifier() {
        return identifier;
    }
}
