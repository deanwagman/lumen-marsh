package com.deanwagman.lumenmarsh.venueops.maintenance.application;

public enum MaintenanceWorkOrderLifecycle {
    ACTIVE,
    TERMINAL,
    ALL;

    public static MaintenanceWorkOrderLifecycle fromQuery(String value) {
        if (value == null || value.isBlank()) {
            return ALL;
        }
        return switch (value.trim().toLowerCase()) {
            case "active" -> ACTIVE;
            case "terminal" -> TERMINAL;
            default -> ALL;
        };
    }
}
