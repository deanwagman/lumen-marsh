package com.deanwagman.lumenmarsh.venueops.maintenance.domain.checklist;

public enum ChecklistResult {
    PENDING,
    PASSED,
    FAILED,
    NOT_APPLICABLE;

    public boolean isResolved() {
        return this == PASSED || this == NOT_APPLICABLE;
    }

    public boolean blocksApproval() {
        return this == FAILED || this == PENDING;
    }
}
