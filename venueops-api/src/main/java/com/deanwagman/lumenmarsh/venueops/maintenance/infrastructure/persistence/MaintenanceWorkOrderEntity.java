package com.deanwagman.lumenmarsh.venueops.maintenance.infrastructure.persistence;

import com.deanwagman.lumenmarsh.venueops.maintenance.domain.workorder.MaintenanceClassification;
import com.deanwagman.lumenmarsh.venueops.maintenance.domain.workorder.MaintenancePriority;
import com.deanwagman.lumenmarsh.venueops.maintenance.domain.workorder.MaintenanceSourceType;
import com.deanwagman.lumenmarsh.venueops.maintenance.domain.workorder.MaintenanceWorkOrderStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "maintenance_work_orders")
public class MaintenanceWorkOrderEntity {

    @Id
    @Column(length = 36)
    private String id;

    @Column(name = "work_order_number", nullable = false, unique = true, length = 32)
    private String workOrderNumber;

    @Column(name = "asset_id", nullable = false, length = 36)
    private String assetId;

    @Column(name = "attraction_id", nullable = false, length = 64)
    private String attractionId;

    @Column(name = "incident_id", length = 36)
    private String incidentId;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false, length = 32)
    private MaintenanceSourceType sourceType;

    @Column(name = "source_reference_id", length = 128)
    private String sourceReferenceId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private MaintenanceClassification classification;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 8)
    private MaintenancePriority priority;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private MaintenanceWorkOrderStatus status;

    @Column(nullable = false)
    private String summary;

    @Column(columnDefinition = "text")
    private String description;

    @Column(name = "assigned_team", length = 128)
    private String assignedTeam;

    @Column(name = "assigned_actor_subject")
    private String assignedActorSubject;

    @Column(name = "estimated_restore_at")
    private Instant estimatedRestoreAt;

    @Column(name = "opened_at")
    private Instant openedAt;

    @Column(name = "work_started_at")
    private Instant workStartedAt;

    @Column(name = "ready_for_testing_at")
    private Instant readyForTestingAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "canceled_at")
    private Instant canceledAt;

    @Column(nullable = false)
    private long version;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected MaintenanceWorkOrderEntity() {
    }

    public MaintenanceWorkOrderEntity(
            String id,
            String workOrderNumber,
            String assetId,
            String attractionId,
            String incidentId,
            MaintenanceSourceType sourceType,
            String sourceReferenceId,
            MaintenanceClassification classification,
            MaintenancePriority priority,
            MaintenanceWorkOrderStatus status,
            String summary,
            String description,
            String assignedTeam,
            String assignedActorSubject,
            Instant estimatedRestoreAt,
            Instant openedAt,
            Instant workStartedAt,
            Instant readyForTestingAt,
            Instant completedAt,
            Instant canceledAt,
            long version,
            Instant createdAt,
            Instant updatedAt
    ) {
        this.id = id;
        this.workOrderNumber = workOrderNumber;
        this.assetId = assetId;
        this.attractionId = attractionId;
        this.incidentId = incidentId;
        this.sourceType = sourceType;
        this.sourceReferenceId = sourceReferenceId;
        this.classification = classification;
        this.priority = priority;
        this.status = status;
        this.summary = summary;
        this.description = description;
        this.assignedTeam = assignedTeam;
        this.assignedActorSubject = assignedActorSubject;
        this.estimatedRestoreAt = estimatedRestoreAt;
        this.openedAt = openedAt;
        this.workStartedAt = workStartedAt;
        this.readyForTestingAt = readyForTestingAt;
        this.completedAt = completedAt;
        this.canceledAt = canceledAt;
        this.version = version;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public String getId() {
        return id;
    }

    public String getWorkOrderNumber() {
        return workOrderNumber;
    }

    public String getAssetId() {
        return assetId;
    }

    public String getAttractionId() {
        return attractionId;
    }

    public String getIncidentId() {
        return incidentId;
    }

    public MaintenanceSourceType getSourceType() {
        return sourceType;
    }

    public String getSourceReferenceId() {
        return sourceReferenceId;
    }

    public MaintenanceClassification getClassification() {
        return classification;
    }

    public MaintenancePriority getPriority() {
        return priority;
    }

    public MaintenanceWorkOrderStatus getStatus() {
        return status;
    }

    public String getSummary() {
        return summary;
    }

    public String getDescription() {
        return description;
    }

    public String getAssignedTeam() {
        return assignedTeam;
    }

    public String getAssignedActorSubject() {
        return assignedActorSubject;
    }

    public Instant getEstimatedRestoreAt() {
        return estimatedRestoreAt;
    }

    public Instant getOpenedAt() {
        return openedAt;
    }

    public Instant getWorkStartedAt() {
        return workStartedAt;
    }

    public Instant getReadyForTestingAt() {
        return readyForTestingAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public Instant getCanceledAt() {
        return canceledAt;
    }

    public long getVersion() {
        return version;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
