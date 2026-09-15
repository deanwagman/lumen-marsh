package com.deanwagman.lumenmarsh.venueops.maintenance.api;

import com.deanwagman.lumenmarsh.venueops.maintenance.domain.workorder.ChecklistItemDraft;
import com.deanwagman.lumenmarsh.venueops.maintenance.domain.workorder.MaintenanceClassification;
import com.deanwagman.lumenmarsh.venueops.maintenance.domain.workorder.MaintenancePriority;
import com.deanwagman.lumenmarsh.venueops.maintenance.domain.workorder.MaintenanceSourceType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public record CreateWorkOrderRequest(
        @NotNull UUID commandId,
        @NotNull UUID assetId,
        UUID incidentId,
        MaintenanceSourceType sourceType,
        String sourceReferenceId,
        @NotNull MaintenanceClassification classification,
        @NotNull MaintenancePriority priority,
        @NotBlank String summary,
        String description,
        @Valid List<CreateChecklistItemRequest> checklist
) {
    public List<ChecklistItemDraft> checklistDrafts() {
        if (checklist == null) {
            return List.of();
        }
        return checklist.stream()
                .map(item -> new ChecklistItemDraft(item.sequence(), item.label(), item.instructions(), item.required()))
                .toList();
    }

    public record CreateChecklistItemRequest(
            int sequence,
            @NotBlank String label,
            String instructions,
            boolean required
    ) {
    }
}
