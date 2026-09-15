package com.deanwagman.lumenmarsh.venueops.maintenance.infrastructure;

import com.deanwagman.lumenmarsh.venueops.attraction.domain.AttractionId;
import com.deanwagman.lumenmarsh.venueops.maintenance.domain.asset.AssetCriticality;
import com.deanwagman.lumenmarsh.venueops.maintenance.domain.asset.AssetServiceStatus;
import com.deanwagman.lumenmarsh.venueops.maintenance.domain.asset.AssetType;
import com.deanwagman.lumenmarsh.venueops.maintenance.domain.asset.MaintenanceAsset;
import com.deanwagman.lumenmarsh.venueops.maintenance.domain.asset.MaintenanceAssetId;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class MaintenanceAssetSeedData {

    public static final MaintenanceAssetId CYPRESS_COIL_ATTRACTION =
            MaintenanceAssetId.of("a1111111-1111-4111-8111-111111111111");
    public static final MaintenanceAssetId CYPRESS_COIL_RIDE_SYSTEM =
            MaintenanceAssetId.of("a2222222-2222-4222-8222-222222222222");
    public static final MaintenanceAssetId CYPRESS_COIL_TRAIN_01 =
            MaintenanceAssetId.of("a3333333-3333-4333-8333-333333333333");
    public static final MaintenanceAssetId CYPRESS_COIL_WHEEL_A =
            MaintenanceAssetId.of("0fd7c7ce-f7af-4b65-8789-27679ca40303");
    public static final MaintenanceAssetId CYPRESS_COIL_SENSOR =
            MaintenanceAssetId.of("a5555555-5555-4555-8555-555555555555");

    private MaintenanceAssetSeedData() {
    }

    public static List<MaintenanceAsset> assets(Clock clock) {
        Instant now = Instant.now(clock);
        AttractionId cypress = new AttractionId("cypress-coil");
        return List.of(
                asset(
                        CYPRESS_COIL_ATTRACTION,
                        "CC-ATTRACTION",
                        "Cypress Coil",
                        AssetType.ATTRACTION,
                        cypress,
                        null,
                        AssetCriticality.CRITICAL,
                        now
                ),
                asset(
                        CYPRESS_COIL_RIDE_SYSTEM,
                        "CC-RIDE-SYSTEM",
                        "Cypress Coil Ride System",
                        AssetType.SYSTEM,
                        cypress,
                        CYPRESS_COIL_ATTRACTION,
                        AssetCriticality.CRITICAL,
                        now
                ),
                asset(
                        CYPRESS_COIL_TRAIN_01,
                        "CC-TRAIN-01",
                        "Cypress Coil Train 1",
                        AssetType.VEHICLE,
                        cypress,
                        CYPRESS_COIL_RIDE_SYSTEM,
                        AssetCriticality.HIGH,
                        now
                ),
                asset(
                        CYPRESS_COIL_WHEEL_A,
                        "CC-TRAIN-01-WHEEL-A",
                        "Cypress Coil Train 1 Wheel Assembly A",
                        AssetType.COMPONENT,
                        cypress,
                        CYPRESS_COIL_TRAIN_01,
                        AssetCriticality.HIGH,
                        now
                ),
                asset(
                        CYPRESS_COIL_SENSOR,
                        "CC-TRAIN-01-VIB-01",
                        "Cypress Coil Train 1 Vibration Sensor",
                        AssetType.SENSOR,
                        cypress,
                        CYPRESS_COIL_TRAIN_01,
                        AssetCriticality.MEDIUM,
                        now
                )
        );
    }

    private static MaintenanceAsset asset(
            MaintenanceAssetId id,
            String code,
            String name,
            AssetType type,
            AttractionId attractionId,
            MaintenanceAssetId parent,
            AssetCriticality criticality,
            Instant now
    ) {
        return new MaintenanceAsset(
                id,
                code,
                name,
                type,
                attractionId,
                parent,
                criticality,
                AssetServiceStatus.IN_SERVICE,
                "Lumen Marsh Fabrication",
                type == AssetType.SENSOR ? "VIB-440" : "CC-2024",
                Instant.parse("2026-03-01T00:00:00Z"),
                1L,
                now,
                now
        );
    }

    public static UUID wheelAId() {
        return CYPRESS_COIL_WHEEL_A.value();
    }
}
