package com.deanwagman.lumenmarsh.venueops.maintenance.application;

import com.deanwagman.lumenmarsh.venueops.attraction.domain.AttractionId;
import com.deanwagman.lumenmarsh.venueops.maintenance.domain.asset.MaintenanceAssetId;
import com.deanwagman.lumenmarsh.venueops.maintenance.domain.workorder.ChecklistItemDraft;
import com.deanwagman.lumenmarsh.venueops.maintenance.domain.workorder.MaintenanceClassification;
import com.deanwagman.lumenmarsh.venueops.maintenance.domain.workorder.MaintenancePriority;
import com.deanwagman.lumenmarsh.venueops.maintenance.domain.workorder.MaintenanceSourceType;
import com.deanwagman.lumenmarsh.venueops.maintenance.domain.workorder.MaintenanceWorkOrder;
import com.deanwagman.lumenmarsh.venueops.maintenance.domain.workorder.MaintenanceWorkOrderId;
import com.deanwagman.lumenmarsh.venueops.maintenance.infrastructure.InMemoryMaintenanceWorkOrderRepository;
import com.deanwagman.lumenmarsh.venueops.security.ActorIdentity;
import com.deanwagman.lumenmarsh.venueops.security.ActorType;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class MaintenanceWorkOrderQueryServiceTest {

    private static final Instant NOW = Instant.parse("2026-09-14T18:30:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);
    private static final ActorIdentity SUPERVISOR = new ActorIdentity(
            "supervisor-sub-1",
            "Supervisor One",
            ActorType.HUMAN,
            "venueops"
    );
    private static final ActorIdentity OPERATOR = new ActorIdentity(
            "operator-sub-1",
            "Morgan Reyes",
            ActorType.HUMAN,
            "venueops"
    );

    @Test
    void activeLifecycleExcludesTerminalWorkOrdersBeforePaging() {
        InMemoryMaintenanceWorkOrderRepository repository = new InMemoryMaintenanceWorkOrderRepository();
        MaintenanceWorkOrderQueryService service = new MaintenanceWorkOrderQueryService(repository);
        MaintenanceWorkOrder draft = draft("6dbb04f2-b20d-4b16-ae57-43072fc2e408", "LM-2026-0042");
        MaintenanceWorkOrder canceled = draft("9aae15c3-c31e-4c27-bf68-54183fd3f519", "LM-2026-0099");
        canceled.open(OPERATOR, UUID.randomUUID(), "corr", null, CLOCK);
        canceled.cancel(SUPERVISOR, UUID.randomUUID(), "corr", "No longer required", CLOCK, true);
        repository.save(draft);
        repository.save(canceled);

        PageResult<MaintenanceWorkOrder> active = service.list(new MaintenanceWorkOrderFilter(
                null, null, null, null, null, null, null, null, null,
                MaintenanceWorkOrderLifecycle.ACTIVE,
                0,
                1
        ));
        PageResult<MaintenanceWorkOrder> terminal = service.list(new MaintenanceWorkOrderFilter(
                null, null, null, null, null, null, null, null, null,
                MaintenanceWorkOrderLifecycle.TERMINAL,
                0,
                1
        ));

        assertThat(active.total()).isEqualTo(1);
        assertThat(active.items()).extracting(workOrder -> workOrder.workOrderNumber())
                .containsExactly("LM-2026-0042");
        assertThat(terminal.total()).isEqualTo(1);
        assertThat(terminal.items()).extracting(workOrder -> workOrder.workOrderNumber())
                .containsExactly("LM-2026-0099");
    }

    private static MaintenanceWorkOrder draft(String id, String number) {
        return MaintenanceWorkOrder.draft(
                MaintenanceWorkOrderId.of(id),
                number,
                MaintenanceAssetId.of("0fd7c7ce-f7af-4b65-8789-27679ca40303"),
                new AttractionId("cypress-coil"),
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
                UUID.randomUUID(),
                "corr",
                CLOCK
        );
    }
}
