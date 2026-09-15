package com.deanwagman.lumenmarsh.venueops.maintenance.infrastructure;

import com.deanwagman.lumenmarsh.venueops.maintenance.domain.recommendation.MaintenanceRecommendation;
import com.deanwagman.lumenmarsh.venueops.maintenance.domain.recommendation.MaintenanceRecommendationId;
import com.deanwagman.lumenmarsh.venueops.maintenance.domain.recommendation.MaintenanceRecommendationSeverity;
import com.deanwagman.lumenmarsh.venueops.maintenance.domain.recommendation.MaintenanceSignalType;

import java.time.Clock;
import java.time.Instant;

public final class MaintenanceRecommendationSeedData {

    public static final MaintenanceRecommendationId CYPRESS_COIL_VIBRATION =
            MaintenanceRecommendationId.of("b7e2c1a0-4c11-4c11-8c11-27679ca40303");

    public static final String OBSERVATION_ID = "vibration-cc-train-01-seed-20260914T182500Z";

    private MaintenanceRecommendationSeedData() {
    }

    public static MaintenanceRecommendation cypressCoilVibration(Clock clock) {
        return MaintenanceRecommendation.receive(
                CYPRESS_COIL_VIBRATION,
                OBSERVATION_ID,
                Instant.parse("2026-09-14T18:25:00Z"),
                "CC-TRAIN-01-WHEEL-A",
                MaintenanceAssetSeedData.CYPRESS_COIL_WHEEL_A,
                MaintenanceSignalType.VIBRATION,
                MaintenanceRecommendationSeverity.CRITICAL,
                18.4,
                "mm/s",
                "Fictional simulated vibration exceeded the demonstration threshold for three consecutive samples.",
                "Inspect the wheel assembly and verify sensor calibration before return to service.",
                clock
        );
    }
}
