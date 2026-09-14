package com.deanwagman.lumenmarsh.venueops.incident.infrastructure.persistence;

import com.deanwagman.lumenmarsh.venueops.incident.domain.IncidentSeverity;
import com.deanwagman.lumenmarsh.venueops.incident.domain.IncidentStatus;
import com.deanwagman.lumenmarsh.venueops.incident.domain.IncidentType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "incident")
public class IncidentEntity {

    @Id
    @Column(length = 36)
    private String id;

    @Column(nullable = false)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 64)
    private IncidentType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 64)
    private IncidentSeverity severity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 64)
    private IncidentStatus status;

    @Column(name = "internal_description", columnDefinition = "text")
    private String internalDescription;

    @Column(name = "assigned_to")
    private String assignedTo;

    @Column(name = "guest_title")
    private String guestTitle;

    @Column(name = "guest_message", columnDefinition = "text")
    private String guestMessage;

    @Column(name = "guest_advisory_published", nullable = false)
    private boolean guestAdvisoryPublished;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(nullable = false)
    private long version;

    protected IncidentEntity() {
    }

    public IncidentEntity(
            String id,
            String title,
            IncidentType type,
            IncidentSeverity severity,
            IncidentStatus status,
            String internalDescription,
            String assignedTo,
            String guestTitle,
            String guestMessage,
            boolean guestAdvisoryPublished,
            Instant createdAt,
            Instant updatedAt,
            long version
    ) {
        this.id = id;
        this.title = title;
        this.type = type;
        this.severity = severity;
        this.status = status;
        this.internalDescription = internalDescription;
        this.assignedTo = assignedTo;
        this.guestTitle = guestTitle;
        this.guestMessage = guestMessage;
        this.guestAdvisoryPublished = guestAdvisoryPublished;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.version = version;
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public IncidentType getType() {
        return type;
    }

    public IncidentSeverity getSeverity() {
        return severity;
    }

    public IncidentStatus getStatus() {
        return status;
    }

    public String getInternalDescription() {
        return internalDescription;
    }

    public String getAssignedTo() {
        return assignedTo;
    }

    public String getGuestTitle() {
        return guestTitle;
    }

    public String getGuestMessage() {
        return guestMessage;
    }

    public boolean isGuestAdvisoryPublished() {
        return guestAdvisoryPublished;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public long getVersion() {
        return version;
    }
}
