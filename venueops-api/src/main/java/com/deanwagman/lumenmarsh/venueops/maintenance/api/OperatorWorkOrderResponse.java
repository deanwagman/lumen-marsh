package com.deanwagman.lumenmarsh.venueops.maintenance.api;

import com.deanwagman.lumenmarsh.venueops.incident.domain.Incident;
import com.deanwagman.lumenmarsh.venueops.maintenance.domain.asset.MaintenanceAsset;
import com.deanwagman.lumenmarsh.venueops.maintenance.domain.checklist.ChecklistResult;
import com.deanwagman.lumenmarsh.venueops.maintenance.domain.checklist.MaintenanceChecklistItem;
import com.deanwagman.lumenmarsh.venueops.maintenance.domain.workorder.MaintenanceClassification;
import com.deanwagman.lumenmarsh.venueops.maintenance.domain.workorder.MaintenanceEvidence;
import com.deanwagman.lumenmarsh.venueops.maintenance.domain.workorder.MaintenancePriority;
import com.deanwagman.lumenmarsh.venueops.maintenance.domain.workorder.MaintenanceSourceType;
import com.deanwagman.lumenmarsh.venueops.maintenance.domain.workorder.MaintenanceWorkOrder;
import com.deanwagman.lumenmarsh.venueops.maintenance.domain.workorder.MaintenanceWorkOrderStatus;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record OperatorWorkOrderResponse(
        UUID id,
        String workOrderNumber,
        MaintenanceWorkOrderStatus status,
        MaintenancePriority priority,
        MaintenanceClassification classification,
        MaintenanceSourceType sourceType,
        String sourceReferenceId,
        AssetSummary asset,
        String attractionId,
        IncidentSummary incident,
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
        RecommendedAttractionActionResponse recommendedAttractionAction,
        List<ChecklistItemResponse> checklist,
        List<EvidenceResponse> evidence,
        long version,
        Instant createdAt,
        Instant updatedAt
) {
    public static OperatorWorkOrderResponse from(MaintenanceWorkOrder workOrder, MaintenanceAsset asset, Incident incident) {
        MaintenanceWorkOrder.RecommendedAttractionAction recommended = workOrder.recommendedAttractionAction();
        return new OperatorWorkOrderResponse(
                workOrder.id().value(),
                workOrder.workOrderNumber(),
                workOrder.status(),
                workOrder.priority(),
                workOrder.classification(),
                workOrder.sourceType(),
                workOrder.sourceReferenceId(),
                new AssetSummary(asset.id().value(), asset.assetCode(), asset.name()),
                workOrder.attractionId().value(),
                incident == null ? null : new IncidentSummary(
                        incident.id().value(),
                        incident.title(),
                        incident.status().name(),
                        incident.severity().name()
                ),
                workOrder.summary(),
                workOrder.description(),
                workOrder.assignedTeam(),
                workOrder.assignedActorSubject(),
                workOrder.estimatedRestoreAt(),
                workOrder.openedAt(),
                workOrder.workStartedAt(),
                workOrder.readyForTestingAt(),
                workOrder.completedAt(),
                workOrder.canceledAt(),
                recommended == null ? null : new RecommendedAttractionActionResponse(recommended.command(), recommended.reason()),
                workOrder.checklist().stream().map(ChecklistItemResponse::from).toList(),
                workOrder.evidence().stream().map(EvidenceResponse::from).toList(),
                workOrder.version(),
                workOrder.createdAt(),
                workOrder.updatedAt()
        );
    }

    public record AssetSummary(UUID id, String assetCode, String name) {
    }

    public record IncidentSummary(String id, String title, String status, String severity) {
    }

    public record RecommendedAttractionActionResponse(String command, String reason) {
    }

    public record EvidenceResponse(
            UUID id,
            String label,
            String contentType,
            String uri,
            String addedBySubject,
            Instant addedAt
    ) {
        static EvidenceResponse from(MaintenanceEvidence evidence) {
            return new EvidenceResponse(
                    evidence.id(),
                    evidence.label(),
                    evidence.contentType(),
                    evidence.uri(),
                    evidence.addedBySubject(),
                    evidence.addedAt()
            );
        }
    }

    public record ChecklistItemResponse(
            UUID id,
            int sequence,
            String label,
            String instructions,
            boolean required,
            ChecklistResult result,
            String notes,
            String completedByDisplayName,
            Instant completedAt,
            long version
    ) {
        static ChecklistItemResponse from(MaintenanceChecklistItem item) {
            return new ChecklistItemResponse(
                    item.id().value(),
                    item.sequence(),
                    item.label(),
                    item.instructions(),
                    item.required(),
                    item.result(),
                    item.notes(),
                    item.completedByDisplayName(),
                    item.completedAt(),
                    item.version()
            );
        }
    }
}
