package com.deanwagman.lumenmarsh.venueops.maintenance.domain.workorder;

public enum MaintenancePriority {
    P1,
    P2,
    P3,
    P4;

    public boolean requiresSupervisorToCancel() {
        return this == P1 || this == P2;
    }

    public boolean isUrgentCorrective() {
        return this == P1;
    }
}
