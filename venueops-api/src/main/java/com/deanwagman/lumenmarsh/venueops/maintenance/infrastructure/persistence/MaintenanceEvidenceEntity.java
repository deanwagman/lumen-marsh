package com.deanwagman.lumenmarsh.venueops.maintenance.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "maintenance_work_order_evidence")
public class MaintenanceEvidenceEntity {

    @Id
    @Column(length = 36)
    private String id;

    @Column(name = "work_order_id", nullable = false, length = 36)
    private String workOrderId;

    @Column(nullable = false)
    private String label;

    @Column(name = "content_type", nullable = false, length = 128)
    private String contentType;

    @Column(nullable = false, length = 1024)
    private String uri;

    @Column(name = "added_by_subject", nullable = false)
    private String addedBySubject;

    @Column(name = "added_at", nullable = false)
    private Instant addedAt;

    protected MaintenanceEvidenceEntity() {
    }

    public MaintenanceEvidenceEntity(
            String id,
            String workOrderId,
            String label,
            String contentType,
            String uri,
            String addedBySubject,
            Instant addedAt
    ) {
        this.id = id;
        this.workOrderId = workOrderId;
        this.label = label;
        this.contentType = contentType;
        this.uri = uri;
        this.addedBySubject = addedBySubject;
        this.addedAt = addedAt;
    }

    public String getId() {
        return id;
    }

    public String getWorkOrderId() {
        return workOrderId;
    }

    public String getLabel() {
        return label;
    }

    public String getContentType() {
        return contentType;
    }

    public String getUri() {
        return uri;
    }

    public String getAddedBySubject() {
        return addedBySubject;
    }

    public Instant getAddedAt() {
        return addedAt;
    }
}
