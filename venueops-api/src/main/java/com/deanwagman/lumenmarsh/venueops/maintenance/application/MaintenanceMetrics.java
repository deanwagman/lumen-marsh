package com.deanwagman.lumenmarsh.venueops.maintenance.application;

import com.deanwagman.lumenmarsh.venueops.maintenance.domain.workorder.MaintenanceWorkOrder;
import com.deanwagman.lumenmarsh.venueops.maintenance.domain.workorder.MaintenanceWorkOrderCommand;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

import java.time.Duration;
import java.util.Objects;

public class MaintenanceMetrics {

    private final MaintenanceWorkOrderRepository workOrders;
    private final MaintenanceRecommendationRepository recommendations;
    private final Counter created;
    private final Counter completed;
    private final Counter rejected;
    private final Counter stale;
    private final Timer timeToAssign;
    private final Timer timeToRepair;
    private final Timer downtime;

    public MaintenanceMetrics(
            MeterRegistry meterRegistry,
            MaintenanceWorkOrderRepository workOrders,
            MaintenanceRecommendationRepository recommendations
    ) {
        this.workOrders = Objects.requireNonNull(workOrders);
        this.recommendations = Objects.requireNonNull(recommendations);
        this.created = Counter.builder("venueops.maintenance.work.orders.created")
                .register(meterRegistry);
        this.completed = Counter.builder("venueops.maintenance.work.orders.completed")
                .register(meterRegistry);
        this.rejected = Counter.builder("venueops.maintenance.transition.rejected")
                .register(meterRegistry);
        this.stale = Counter.builder("venueops.maintenance.stale.command")
                .register(meterRegistry);
        this.timeToAssign = Timer.builder("venueops.maintenance.time.to.assign")
                .register(meterRegistry);
        this.timeToRepair = Timer.builder("venueops.maintenance.time.to.repair")
                .register(meterRegistry);
        this.downtime = Timer.builder("venueops.attraction.downtime")
                .register(meterRegistry);
        Gauge.builder("venueops.maintenance.work.orders.active", this, MaintenanceMetrics::activeCount)
                .register(meterRegistry);
        Gauge.builder("venueops.maintenance.recommendations.pending", this, MaintenanceMetrics::pendingRecommendations)
                .register(meterRegistry);
    }

    public void created(MaintenanceWorkOrder workOrder) {
        created.increment();
        touch(workOrder);
    }

    public void stale() {
        stale.increment();
    }

    public void rejected(MaintenanceWorkOrderCommand command) {
        rejected.increment();
        touch(command);
    }

    public void transitioned(
            MaintenanceWorkOrder workOrder,
            MaintenanceWorkOrderCommand command,
            com.deanwagman.lumenmarsh.venueops.maintenance.domain.activity.MaintenanceActivity activity
    ) {
        if (command == MaintenanceWorkOrderCommand.ASSIGN && workOrder.openedAt() != null) {
            timeToAssign.record(Duration.between(workOrder.openedAt(), activity.occurredAt()));
        }
        if (command == MaintenanceWorkOrderCommand.APPROVE_INSPECTION && workOrder.workStartedAt() != null) {
            timeToRepair.record(Duration.between(workOrder.workStartedAt(), activity.occurredAt()));
        }
        if (command == MaintenanceWorkOrderCommand.COMPLETE) {
            completed.increment();
            if (workOrder.workStartedAt() != null) {
                downtime.record(Duration.between(workOrder.workStartedAt(), activity.occurredAt()));
            }
        }
        touch(workOrder);
        touch(command);
    }

    private double activeCount() {
        return workOrders.findAll().stream()
                .filter(workOrder -> workOrder.status().isActive())
                .count();
    }

    private double pendingRecommendations() {
        return recommendations.findAll().stream()
                .filter(recommendation -> recommendation.status().isPending())
                .count();
    }

    private static void touch(Object ignored) {
        // Keep bounded metric labels out of the public API; status/priority are not tag values.
    }
}
