package com.deanwagman.lumenmarsh.venueops.maintenance.infrastructure.persistence;

import com.deanwagman.lumenmarsh.venueops.maintenance.domain.checklist.ChecklistResult;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "maintenance_checklist_items")
public class MaintenanceChecklistItemEntity {

    @Id
    @Column(length = 36)
    private String id;

    @Column(name = "work_order_id", nullable = false, length = 36)
    private String workOrderId;

    @Column(nullable = false)
    private int sequence;

    @Column(nullable = false)
    private String label;

    @Column(columnDefinition = "text")
    private String instructions;

    @Column(nullable = false)
    private boolean required;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private ChecklistResult result;

    @Column(columnDefinition = "text")
    private String notes;

    @Column(name = "completed_by_subject")
    private String completedBySubject;

    @Column(name = "completed_by_display_name")
    private String completedByDisplayName;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(nullable = false)
    private long version;

    protected MaintenanceChecklistItemEntity() {
    }

    public MaintenanceChecklistItemEntity(
            String id,
            String workOrderId,
            int sequence,
            String label,
            String instructions,
            boolean required,
            ChecklistResult result,
            String notes,
            String completedBySubject,
            String completedByDisplayName,
            Instant completedAt,
            long version
    ) {
        this.id = id;
        this.workOrderId = workOrderId;
        this.sequence = sequence;
        this.label = label;
        this.instructions = instructions;
        this.required = required;
        this.result = result;
        this.notes = notes;
        this.completedBySubject = completedBySubject;
        this.completedByDisplayName = completedByDisplayName;
        this.completedAt = completedAt;
        this.version = version;
    }

    public String getId() {
        return id;
    }

    public String getWorkOrderId() {
        return workOrderId;
    }

    public int getSequence() {
        return sequence;
    }

    public String getLabel() {
        return label;
    }

    public String getInstructions() {
        return instructions;
    }

    public boolean isRequired() {
        return required;
    }

    public ChecklistResult getResult() {
        return result;
    }

    public String getNotes() {
        return notes;
    }

    public String getCompletedBySubject() {
        return completedBySubject;
    }

    public String getCompletedByDisplayName() {
        return completedByDisplayName;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public long getVersion() {
        return version;
    }
}
