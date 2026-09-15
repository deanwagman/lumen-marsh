package com.deanwagman.lumenmarsh.venueops.maintenance.domain.workorder;

public record ChecklistItemDraft(
        int sequence,
        String label,
        String instructions,
        boolean required
) {
}
