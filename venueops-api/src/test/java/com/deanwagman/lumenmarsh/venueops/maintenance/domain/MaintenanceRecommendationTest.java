package com.deanwagman.lumenmarsh.venueops.maintenance.domain.recommendation;

import com.deanwagman.lumenmarsh.venueops.maintenance.domain.asset.MaintenanceAssetId;
import com.deanwagman.lumenmarsh.venueops.maintenance.domain.event.MaintenanceEventType;
import com.deanwagman.lumenmarsh.venueops.maintenance.domain.workorder.MaintenanceWorkOrderId;
import com.deanwagman.lumenmarsh.venueops.security.ActorIdentity;
import com.deanwagman.lumenmarsh.venueops.security.ActorType;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MaintenanceRecommendationTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-09-14T18:25:02Z"), ZoneOffset.UTC);
    private static final ActorIdentity OPERATOR = new ActorIdentity(
            "operator-sub-1",
            "Operator One",
            ActorType.HUMAN,
            "venueops"
    );
    private static final UUID COMMAND_ID = UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee");

    @Test
    void acceptClaimsThenAttachesWorkOrderAndDismissIsTerminal() {
        MaintenanceRecommendation recommendation = pending("vibration-cc-train-01-20260914T182500Z");

        assertThat(recommendation.status()).isEqualTo(MaintenanceRecommendationStatus.PENDING_REVIEW);
        assertThat(recommendation.claimAcceptance(OPERATOR, COMMAND_ID, CLOCK))
                .isEqualTo(MaintenanceEventType.MAINTENANCE_RECOMMENDATION_ACCEPTED);
        assertThat(recommendation.status()).isEqualTo(MaintenanceRecommendationStatus.ACCEPTED);
        assertThat(recommendation.version()).isEqualTo(2);
        assertThat(recommendation.uncommittedBumps()).isEqualTo(1);

        recommendation.attachWorkOrder(MaintenanceWorkOrderId.of("6dbb04f2-b20d-4b65-8789-27679ca40303"), CLOCK);
        assertThat(recommendation.status()).isEqualTo(MaintenanceRecommendationStatus.WORK_ORDER_CREATED);
        assertThat(recommendation.workOrderId().toString()).isEqualTo("6dbb04f2-b20d-4b65-8789-27679ca40303");
        assertThat(recommendation.version()).isEqualTo(3);
        assertThat(recommendation.isTerminalFor(COMMAND_ID)).isTrue();

        MaintenanceRecommendation dismissed = pending("other-observation");
        assertThat(dismissed.dismiss(OPERATOR, UUID.randomUUID(), CLOCK))
                .isEqualTo(MaintenanceEventType.MAINTENANCE_RECOMMENDATION_DISMISSED);
        assertThatThrownBy(() -> dismissed.claimAcceptance(OPERATOR, UUID.randomUUID(), CLOCK))
                .isInstanceOf(InvalidMaintenanceRecommendationTransitionException.class);
    }

    @Test
    void concurrentAcceptanceClaimIsRejectedAfterPendingReviewLeaves() {
        MaintenanceRecommendation recommendation = pending("vibration-already-claimed");
        recommendation.claimAcceptance(OPERATOR, COMMAND_ID, CLOCK);

        assertThatThrownBy(() -> recommendation.claimAcceptance(OPERATOR, UUID.randomUUID(), CLOCK))
                .isInstanceOf(InvalidMaintenanceRecommendationTransitionException.class);
        assertThat(recommendation.canContinueAcceptance(COMMAND_ID)).isTrue();
        assertThat(recommendation.canContinueAcceptance(UUID.randomUUID())).isFalse();
    }

    private static MaintenanceRecommendation pending(String observationId) {
        return MaintenanceRecommendation.receive(
                MaintenanceRecommendationId.random(),
                observationId,
                Instant.parse("2026-09-14T18:25:00Z"),
                "CC-TRAIN-01-WHEEL-A",
                MaintenanceAssetId.of("0fd7c7ce-f7af-4b65-8789-27679ca40303"),
                MaintenanceSignalType.VIBRATION,
                MaintenanceRecommendationSeverity.WARNING,
                8.4,
                "mm/s",
                "Fictional simulated vibration exceeded the demonstration threshold.",
                "Inspect the wheel assembly.",
                CLOCK
        );
    }
}
