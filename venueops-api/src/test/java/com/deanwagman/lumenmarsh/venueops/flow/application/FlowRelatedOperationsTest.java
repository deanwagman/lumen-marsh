package com.deanwagman.lumenmarsh.venueops.flow.application;

import com.deanwagman.lumenmarsh.venueops.attraction.domain.AttractionId;
import com.deanwagman.lumenmarsh.venueops.incident.domain.Incident;
import com.deanwagman.lumenmarsh.venueops.incident.domain.IncidentSeverity;
import com.deanwagman.lumenmarsh.venueops.incident.domain.IncidentType;
import com.deanwagman.lumenmarsh.venueops.incident.infrastructure.InMemoryIncidentRepository;
import com.deanwagman.lumenmarsh.venueops.maintenance.application.MaintenanceWorkOrderRepository;
import com.deanwagman.lumenmarsh.venueops.maintenance.domain.asset.MaintenanceAssetId;
import com.deanwagman.lumenmarsh.venueops.maintenance.domain.workorder.MaintenanceWorkOrder;
import com.deanwagman.lumenmarsh.venueops.maintenance.domain.workorder.MaintenanceWorkOrderId;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class FlowRelatedOperationsTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-09-15T18:30:00Z"), ZoneOffset.UTC);
    private static final AttractionId MANGROVE = new AttractionId("mangrove-run");

    @Test
    void linksOpenIncidentForTheSourceAttraction() {
        InMemoryIncidentRepository incidents = new InMemoryIncidentRepository();
        Incident incident = Incident.report(
                "Technical delay at Mangrove Run",
                IncidentType.TECHNICAL,
                IncidentSeverity.MAJOR,
                "Simulated dispatch fault for the park-flow demo.",
                List.of(MANGROVE),
                "Operator One",
                CLOCK
        );
        incidents.save(incident);

        FlowRelatedOperations operations = new FlowRelatedOperations(incidents, emptyWorkOrders());

        assertThat(operations.linksFor(MANGROVE).incidentId()).isEqualTo(incident.id().value());
        assertThat(operations.linksFor(new AttractionId("cypress-coil")).incidentId()).isNull();
    }

    private static MaintenanceWorkOrderRepository emptyWorkOrders() {
        return new MaintenanceWorkOrderRepository() {
            @Override
            public Optional<MaintenanceWorkOrder> findById(MaintenanceWorkOrderId id) {
                return Optional.empty();
            }

            @Override
            public List<MaintenanceWorkOrder> findAll() {
                return List.of();
            }

            @Override
            public List<MaintenanceWorkOrder> findByAssetId(MaintenanceAssetId assetId) {
                return List.of();
            }

            @Override
            public void save(MaintenanceWorkOrder workOrder) {
            }
        };
    }
}
