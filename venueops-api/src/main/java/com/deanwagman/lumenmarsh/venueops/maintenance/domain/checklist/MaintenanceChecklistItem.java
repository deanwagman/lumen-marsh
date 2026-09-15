package com.deanwagman.lumenmarsh.venueops.maintenance.domain.checklist;

import java.time.Instant;
import java.util.Objects;

public final class MaintenanceChecklistItem {

    private final MaintenanceChecklistItemId id;
    private final int sequence;
    private final String label;
    private final String instructions;
    private final boolean required;
    private ChecklistResult result;
    private String notes;
    private String completedBySubject;
    private String completedByDisplayName;
    private Instant completedAt;
    private long version;

    public MaintenanceChecklistItem(
            MaintenanceChecklistItemId id,
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
        this.id = Objects.requireNonNull(id, "id is required");
        this.sequence = sequence;
        this.label = requireText(label, "label");
        this.instructions = normalizeOptional(instructions);
        this.required = required;
        this.result = Objects.requireNonNull(result, "result is required");
        this.notes = normalizeOptional(notes);
        this.completedBySubject = normalizeOptional(completedBySubject);
        this.completedByDisplayName = normalizeOptional(completedByDisplayName);
        this.completedAt = completedAt;
        this.version = version;
    }

    public static MaintenanceChecklistItem pending(
            MaintenanceChecklistItemId id,
            int sequence,
            String label,
            String instructions,
            boolean required
    ) {
        return new MaintenanceChecklistItem(
                id,
                sequence,
                label,
                instructions,
                required,
                ChecklistResult.PENDING,
                null,
                null,
                null,
                null,
                0L
        );
    }

    public void recordResult(
            ChecklistResult nextResult,
            String notes,
            String actorSubject,
            String actorDisplayName,
            Instant completedAt
    ) {
        Objects.requireNonNull(nextResult, "result is required");
        if (nextResult == ChecklistResult.PENDING) {
            throw new IllegalArgumentException("Checklist result must not remain pending");
        }
        this.result = nextResult;
        this.notes = normalizeOptional(notes);
        this.completedBySubject = requireText(actorSubject, "completedBySubject");
        this.completedByDisplayName = requireText(actorDisplayName, "completedByDisplayName");
        this.completedAt = Objects.requireNonNull(completedAt, "completedAt is required");
        this.version = this.version + 1;
    }

    public MaintenanceChecklistItemId id() {
        return id;
    }

    public int sequence() {
        return sequence;
    }

    public String label() {
        return label;
    }

    public String instructions() {
        return instructions;
    }

    public boolean required() {
        return required;
    }

    public ChecklistResult result() {
        return result;
    }

    public String notes() {
        return notes;
    }

    public String completedBySubject() {
        return completedBySubject;
    }

    public String completedByDisplayName() {
        return completedByDisplayName;
    }

    public Instant completedAt() {
        return completedAt;
    }

    public long version() {
        return version;
    }

    public boolean blocksInspectionRequest() {
        return required && !result.isResolved();
    }

    public boolean blocksInspectionApproval() {
        return required && result.blocksApproval();
    }

    public MaintenanceChecklistItem copy() {
        return new MaintenanceChecklistItem(
                id,
                sequence,
                label,
                instructions,
                required,
                result,
                notes,
                completedBySubject,
                completedByDisplayName,
                completedAt,
                version
        );
    }

    private static String normalizeOptional(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static String requireText(String value, String field) {
        Objects.requireNonNull(value, field + " is required");
        if (value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return value.trim();
    }
}
