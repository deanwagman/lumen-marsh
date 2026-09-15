package com.deanwagman.lumenmarsh.venueops.maintenance.domain.workorder;

import com.deanwagman.lumenmarsh.venueops.attraction.domain.AttractionId;
import com.deanwagman.lumenmarsh.venueops.attraction.domain.AttractionStatus;
import com.deanwagman.lumenmarsh.venueops.maintenance.domain.asset.MaintenanceAssetId;
import com.deanwagman.lumenmarsh.venueops.maintenance.domain.checklist.ChecklistResult;
import com.deanwagman.lumenmarsh.venueops.maintenance.domain.event.MaintenanceEventType;
import com.deanwagman.lumenmarsh.venueops.security.ActorIdentity;
import com.deanwagman.lumenmarsh.venueops.security.ActorType;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MaintenanceWorkOrderTest {

    private static final Instant NOW = Instant.parse("2026-09-14T18:30:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);
    private static final ActorIdentity OPERATOR = new ActorIdentity(
            "operator-sub-1",
            "Morgan Reyes",
            ActorType.HUMAN,
            "venueops"
    );
    private static final ActorIdentity SUPERVISOR = new ActorIdentity(
            "supervisor-sub-1",
            "Supervisor One",
            ActorType.HUMAN,
            "venueops"
    );
    private static final MaintenanceAssetId ASSET = MaintenanceAssetId.of("0fd7c7ce-f7af-4b65-8789-27679ca40303");
    private static final AttractionId CYPRESS = new AttractionId("cypress-coil");

    @Test
    void draftStartsOpenableAtVersionOne() {
        MaintenanceWorkOrder workOrder = draft();

        assertThat(workOrder.status()).isEqualTo(MaintenanceWorkOrderStatus.DRAFT);
        assertThat(workOrder.version()).isEqualTo(1);
        assertThat(workOrder.activity()).hasSize(1);
        assertThat(workOrder.activity().getFirst().eventType()).isEqualTo(MaintenanceEventType.WORK_ORDER_CREATED);
        assertThat(workOrder.activity().getFirst().actorDisplayName()).isEqualTo("Morgan Reyes");
        assertThat(workOrder.checklist()).hasSize(2);

        workOrder.open(OPERATOR, commandId(), "corr", null, CLOCK);
        assertThat(workOrder.status()).isEqualTo(MaintenanceWorkOrderStatus.OPEN);
        assertThat(workOrder.openedAt()).isEqualTo(NOW);
        assertThat(workOrder.version()).isEqualTo(2);
    }

    @Test
    void unassignedWorkCannotStart() {
        MaintenanceWorkOrder workOrder = opened();

        assertThatThrownBy(() -> workOrder.startWork(OPERATOR, commandId(), "corr", null, CLOCK))
                .isInstanceOf(InvalidMaintenanceTransitionException.class);
        assertThat(workOrder.status()).isEqualTo(MaintenanceWorkOrderStatus.OPEN);
        assertThat(workOrder.version()).isEqualTo(2);
    }

    @Test
    void workCannotRequestInspectionWithIncompleteRequiredItems() {
        MaintenanceWorkOrder workOrder = inProgress();

        assertThatThrownBy(() -> workOrder.requestInspection(OPERATOR, commandId(), "corr", null, CLOCK))
                .isInstanceOf(MaintenancePrerequisiteException.class);
        assertThat(workOrder.status()).isEqualTo(MaintenanceWorkOrderStatus.IN_PROGRESS);
    }

    @Test
    void failedRequiredItemBlocksApproval() {
        MaintenanceWorkOrder workOrder = awaitingInspection();
        workOrder.recordChecklistResult(
                workOrder.checklist().getFirst().id(),
                ChecklistResult.FAILED,
                "Crack found",
                OPERATOR,
                commandId(),
                "corr",
                "Physical inspection completed.",
                CLOCK
        );

        assertThatThrownBy(() -> workOrder.approveInspection(
                SUPERVISOR,
                commandId(),
                "corr",
                "Should not approve",
                CLOCK,
                true
        )).isInstanceOf(MaintenancePrerequisiteException.class);
        assertThat(workOrder.status()).isEqualTo(MaintenanceWorkOrderStatus.AWAITING_INSPECTION);
    }

    @Test
    void supervisorCanApproveValidInspectionAndOperatorCannot() {
        MaintenanceWorkOrder workOrder = awaitingInspection();

        assertThatThrownBy(() -> workOrder.approveInspection(OPERATOR, commandId(), "corr", "Ready", CLOCK, false))
                .isInstanceOf(MaintenanceSupervisorRequiredException.class);

        workOrder.approveInspection(
                SUPERVISOR,
                commandId(),
                "corr",
                "Required inspection steps passed. Attraction is ready for operational testing.",
                CLOCK,
                true
        );
        assertThat(workOrder.status()).isEqualTo(MaintenanceWorkOrderStatus.READY_FOR_TESTING);
        assertThat(workOrder.readyForTestingAt()).isEqualTo(NOW);
        assertThat(workOrder.activity().getLast().eventType()).isEqualTo(MaintenanceEventType.INSPECTION_APPROVED);
    }

    @Test
    void rejectedInspectionReturnsWorkToInProgress() {
        MaintenanceWorkOrder workOrder = awaitingInspection();
        workOrder.rejectInspection(SUPERVISOR, commandId(), "corr", "Retorque the assembly.", CLOCK, true);

        assertThat(workOrder.status()).isEqualTo(MaintenanceWorkOrderStatus.IN_PROGRESS);
        assertThat(workOrder.activity().getLast().eventType()).isEqualTo(MaintenanceEventType.INSPECTION_REJECTED);
    }

    @Test
    void terminalWorkOrdersRejectCommands() {
        MaintenanceWorkOrder workOrder = canceled();

        assertThatThrownBy(() -> workOrder.assign("ride-maintenance-alpha", null, OPERATOR, commandId(), "corr", null, CLOCK))
                .isInstanceOf(InvalidMaintenanceTransitionException.class);
        assertThatThrownBy(() -> workOrder.reassign("other", null, OPERATOR, commandId(), "corr", "Need another crew", CLOCK))
                .isInstanceOf(InvalidMaintenanceTransitionException.class);
        assertThat(workOrder.status()).isEqualTo(MaintenanceWorkOrderStatus.CANCELED);
    }

    @Test
    void cancellationRequiresReasonAndHighPriorityRequiresSupervisor() {
        MaintenanceWorkOrder p2 = opened();
        assertThatThrownBy(() -> workOrderCancelWithoutReason(p2))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("requires a meaningful reason");

        assertThatThrownBy(() -> p2.cancel(OPERATOR, commandId(), "corr", "Parts delayed", CLOCK, false))
                .isInstanceOf(MaintenanceSupervisorRequiredException.class);

        p2.cancel(SUPERVISOR, commandId(), "corr", "Parts delayed", CLOCK, true);
        assertThat(p2.status()).isEqualTo(MaintenanceWorkOrderStatus.CANCELED);
        assertThat(p2.canceledAt()).isEqualTo(NOW);
    }

    @Test
    void completionCannotBypassAttractionTesting() {
        MaintenanceWorkOrder workOrder = readyForTesting();

        assertThatThrownBy(() -> workOrder.complete(
                SUPERVISOR,
                commandId(),
                "corr",
                "Done",
                CLOCK,
                true,
                AttractionStatus.TESTING
        )).isInstanceOf(MaintenancePrerequisiteException.class);

        assertThatThrownBy(() -> workOrder.complete(
                SUPERVISOR,
                commandId(),
                "corr",
                "Done",
                CLOCK,
                true,
                AttractionStatus.TECHNICAL_DELAY
        )).isInstanceOf(MaintenancePrerequisiteException.class);

        assertThatThrownBy(() -> workOrder.complete(
                SUPERVISOR,
                commandId(),
                "corr",
                "Done",
                CLOCK,
                true,
                AttractionStatus.RETURNING_TO_SERVICE
        )).isInstanceOf(MaintenancePrerequisiteException.class);

        workOrder.complete(SUPERVISOR, commandId(), "corr", "Return-to-service approved", CLOCK, true, AttractionStatus.OPERATING);
        assertThat(workOrder.status()).isEqualTo(MaintenanceWorkOrderStatus.COMPLETED);
        assertThat(workOrder.recommendedAttractionAction()).isNull();
    }

    @Test
    void completionAllowsClosedAttractionsForOvernightWork() {
        MaintenanceWorkOrder workOrder = readyForTesting();
        workOrder.complete(SUPERVISOR, commandId(), "corr", "Overnight work finished", CLOCK, true, AttractionStatus.CLOSED);
        assertThat(workOrder.status()).isEqualTo(MaintenanceWorkOrderStatus.COMPLETED);
    }

    @Test
    void completionAllowsWeatherHoldBecauseItIsIndependentOfTesting() {
        MaintenanceWorkOrder workOrder = readyForTesting();
        workOrder.complete(
                SUPERVISOR,
                commandId(),
                "corr",
                "Maintenance finished while the attraction remains on weather hold",
                CLOCK,
                true,
                AttractionStatus.WEATHER_HOLD
        );
        assertThat(workOrder.status()).isEqualTo(MaintenanceWorkOrderStatus.COMPLETED);
    }

    @Test
    void p1CorrectiveWorkRecommendsTechnicalFaultWithoutChangingAttractionState() {
        MaintenanceWorkOrder workOrder = MaintenanceWorkOrder.draft(
                MaintenanceWorkOrderId.of("6dbb04f2-b20d-4b16-ae57-43072fc2e408"),
                "LM-2026-0042",
                ASSET,
                CYPRESS,
                null,
                MaintenanceSourceType.MANUAL,
                null,
                MaintenanceClassification.CORRECTIVE,
                MaintenancePriority.P1,
                "Investigate elevated wheel vibration",
                "Telemetry review indicates sustained vibration above the fictional demonstration threshold.",
                List.of(new ChecklistItemDraft(1, "Inspect wheel assembly", null, true)),
                OPERATOR,
                commandId(),
                "corr",
                CLOCK
        );
        assertThat(workOrder.recommendedAttractionAction().command()).isEqualTo("REPORT_TECHNICAL_FAULT");
        assertThat(workOrder.recommendedAttractionAction().reason()).contains("LM-2026-0042");
        assertThat(workOrder.status()).isEqualTo(MaintenanceWorkOrderStatus.DRAFT);
    }

    @Test
    void p1WorkReadyForTestingRecommendsStartTesting() {
        MaintenanceWorkOrder workOrder = p1ReadyForTesting();
        assertThat(workOrder.status()).isEqualTo(MaintenanceWorkOrderStatus.READY_FOR_TESTING);
        assertThat(workOrder.recommendedAttractionAction().command()).isEqualTo("START_TESTING");
        assertThat(workOrder.recommendedAttractionAction().reason()).contains("LM-2026-0099");
    }

    @Test
    void assignRequiresTeamOrActor() {
        MaintenanceWorkOrder workOrder = opened();
        assertThatThrownBy(() -> workOrder.assign(null, null, OPERATOR, commandId(), "corr", null, CLOCK))
                .isInstanceOf(MaintenancePrerequisiteException.class);
    }

    private static void workOrderCancelWithoutReason(MaintenanceWorkOrder workOrder) {
        workOrder.cancel(SUPERVISOR, commandId(), "corr", "  ", CLOCK, true);
    }

    private static MaintenanceWorkOrder draft() {
        return MaintenanceWorkOrder.draft(
                MaintenanceWorkOrderId.of("6dbb04f2-b20d-4b16-ae57-43072fc2e408"),
                "LM-2026-0042",
                ASSET,
                CYPRESS,
                null,
                MaintenanceSourceType.MANUAL,
                null,
                MaintenanceClassification.CORRECTIVE,
                MaintenancePriority.P2,
                "Investigate elevated wheel vibration",
                "Telemetry review indicates sustained vibration above the fictional demonstration threshold.",
                List.of(
                        new ChecklistItemDraft(1, "Inspect wheel assembly", null, true),
                        new ChecklistItemDraft(2, "Verify sensor calibration", null, true)
                ),
                OPERATOR,
                commandId(),
                "corr",
                CLOCK
        );
    }

    private static MaintenanceWorkOrder opened() {
        MaintenanceWorkOrder workOrder = draft();
        workOrder.open(OPERATOR, commandId(), "corr", null, CLOCK);
        return workOrder;
    }

    private static MaintenanceWorkOrder inProgress() {
        MaintenanceWorkOrder workOrder = opened();
        workOrder.assign("ride-maintenance-alpha", null, OPERATOR, commandId(), "corr", null, CLOCK);
        workOrder.startWork(OPERATOR, commandId(), "corr", null, CLOCK);
        return workOrder;
    }

    private static MaintenanceWorkOrder awaitingInspection() {
        MaintenanceWorkOrder workOrder = inProgress();
        for (var item : workOrder.checklist()) {
            workOrder.recordChecklistResult(
                    item.id(),
                    ChecklistResult.PASSED,
                    "No visible damage",
                    OPERATOR,
                    commandId(),
                    "corr",
                    "Physical inspection completed.",
                    CLOCK
            );
        }
        workOrder.requestInspection(OPERATOR, commandId(), "corr", null, CLOCK);
        return workOrder;
    }

    private static MaintenanceWorkOrder readyForTesting() {
        MaintenanceWorkOrder workOrder = awaitingInspection();
        workOrder.approveInspection(SUPERVISOR, commandId(), "corr", "Inspection passed", CLOCK, true);
        return workOrder;
    }

    private static MaintenanceWorkOrder p1ReadyForTesting() {
        MaintenanceWorkOrder workOrder = MaintenanceWorkOrder.draft(
                MaintenanceWorkOrderId.of("9aae15c3-c31e-4c27-bf68-54183fd3f519"),
                "LM-2026-0099",
                ASSET,
                CYPRESS,
                null,
                MaintenanceSourceType.MANUAL,
                null,
                MaintenanceClassification.CORRECTIVE,
                MaintenancePriority.P1,
                "Replace failed wheel bearing",
                "P1 corrective work that later becomes ready for testing.",
                List.of(
                        new ChecklistItemDraft(1, "Inspect wheel assembly", null, true),
                        new ChecklistItemDraft(2, "Verify sensor calibration", null, true)
                ),
                OPERATOR,
                commandId(),
                "corr",
                CLOCK
        );
        workOrder.open(OPERATOR, commandId(), "corr", null, CLOCK);
        workOrder.assign("ride-maintenance-alpha", null, OPERATOR, commandId(), "corr", null, CLOCK);
        workOrder.startWork(OPERATOR, commandId(), "corr", null, CLOCK);
        for (var item : workOrder.checklist()) {
            workOrder.recordChecklistResult(
                    item.id(),
                    ChecklistResult.PASSED,
                    "No visible damage",
                    OPERATOR,
                    commandId(),
                    "corr",
                    "Physical inspection completed.",
                    CLOCK
            );
        }
        workOrder.requestInspection(OPERATOR, commandId(), "corr", null, CLOCK);
        workOrder.approveInspection(SUPERVISOR, commandId(), "corr", "Inspection passed", CLOCK, true);
        return workOrder;
    }

    private static MaintenanceWorkOrder canceled() {
        MaintenanceWorkOrder workOrder = opened();
        workOrder.cancel(SUPERVISOR, commandId(), "corr", "No longer required", CLOCK, true);
        return workOrder;
    }

    private static UUID commandId() {
        return UUID.randomUUID();
    }
}
