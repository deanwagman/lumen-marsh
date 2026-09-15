package com.deanwagman.lumenmarsh.venueops.maintenance.application;

import com.deanwagman.lumenmarsh.venueops.maintenance.domain.asset.MaintenanceAsset;
import com.deanwagman.lumenmarsh.venueops.maintenance.domain.recommendation.InvalidMaintenanceRecommendationTransitionException;
import com.deanwagman.lumenmarsh.venueops.maintenance.domain.recommendation.MaintenanceRecommendation;
import com.deanwagman.lumenmarsh.venueops.maintenance.domain.recommendation.MaintenanceRecommendationCommand;
import com.deanwagman.lumenmarsh.venueops.maintenance.domain.recommendation.MaintenanceRecommendationId;
import com.deanwagman.lumenmarsh.venueops.maintenance.domain.recommendation.MaintenanceRecommendationSeverity;
import com.deanwagman.lumenmarsh.venueops.maintenance.domain.recommendation.MaintenanceRecommendationStatus;
import com.deanwagman.lumenmarsh.venueops.maintenance.domain.recommendation.MaintenanceSignalType;
import com.deanwagman.lumenmarsh.venueops.maintenance.domain.workorder.ChecklistItemDraft;
import com.deanwagman.lumenmarsh.venueops.maintenance.domain.workorder.MaintenanceClassification;
import com.deanwagman.lumenmarsh.venueops.maintenance.domain.workorder.MaintenancePriority;
import com.deanwagman.lumenmarsh.venueops.maintenance.domain.workorder.MaintenanceSourceType;
import com.deanwagman.lumenmarsh.venueops.security.ActorIdentity;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class MaintenanceRecommendationService {

    private final MaintenanceRecommendationRepository recommendations;
    private final MaintenanceAssetRepository assets;
    private final MaintenanceWorkOrderCommandService workOrders;
    private final Clock clock;

    public MaintenanceRecommendationService(
            MaintenanceRecommendationRepository recommendations,
            MaintenanceAssetRepository assets,
            MaintenanceWorkOrderCommandService workOrders,
            Clock clock
    ) {
        this.recommendations = Objects.requireNonNull(recommendations);
        this.assets = Objects.requireNonNull(assets);
        this.workOrders = Objects.requireNonNull(workOrders);
        this.clock = Objects.requireNonNull(clock);
    }

    public List<MaintenanceRecommendation> list() {
        return recommendations.findAll();
    }

    public MaintenanceRecommendation get(MaintenanceRecommendationId id) {
        return recommendations.findById(id).orElseThrow(() -> new MaintenanceRecommendationNotFoundException(id));
    }

    @Transactional
    public RecommendationIngestResult ingest(
            String observationId,
            Instant observedAt,
            String assetCode,
            MaintenanceSignalType signalType,
            MaintenanceRecommendationSeverity severity,
            Double value,
            String unit,
            String evidence,
            String recommendedAction
    ) {
        return recommendations.findByObservationId(observationId)
                .map(existing -> new RecommendationIngestResult(existing, true))
                .orElseGet(() -> create(
                        observationId,
                        observedAt,
                        assetCode,
                        signalType,
                        severity,
                        value,
                        unit,
                        evidence,
                        recommendedAction
                ));
    }

    @Transactional
    public MaintenanceRecommendation execute(
            MaintenanceRecommendationId id,
            UUID commandId,
            long expectedVersion,
            MaintenanceRecommendationCommand command,
            ActorIdentity actor,
            String correlationId
    ) {
        recommendations.findByCommandId(commandId)
                .filter(existing -> !existing.id().equals(id))
                .ifPresent(existing -> {
                    throw new ConflictingMaintenanceCommandException(
                            commandId,
                            existing.id().toString(),
                            id.toString()
                    );
                });
        MaintenanceRecommendation recommendation = get(id);
        if (recommendation.isTerminalFor(commandId)) {
            return recommendation;
        }
        switch (command) {
            case DISMISS -> dismiss(recommendation, commandId, expectedVersion, actor);
            case ACCEPT -> accept(recommendation, commandId, expectedVersion, actor, correlationId);
        }
        return recommendation;
    }

    private void dismiss(
            MaintenanceRecommendation recommendation,
            UUID commandId,
            long expectedVersion,
            ActorIdentity actor
    ) {
        requireExpectedVersion(recommendation, expectedVersion);
        recommendation.dismiss(actor, commandId, clock);
        recommendations.save(recommendation);
    }

    private void accept(
            MaintenanceRecommendation recommendation,
            UUID commandId,
            long expectedVersion,
            ActorIdentity actor,
            String correlationId
    ) {
        if (recommendation.status() == MaintenanceRecommendationStatus.PENDING_REVIEW) {
            requireExpectedVersion(recommendation, expectedVersion);
            recommendation.claimAcceptance(actor, commandId, clock);
            recommendations.save(recommendation);
        } else if (!recommendation.canContinueAcceptance(commandId)) {
            throw new InvalidMaintenanceRecommendationTransitionException(
                    recommendation.status(),
                    MaintenanceRecommendationCommand.ACCEPT
            );
        }
        MaintenanceCommandResult created = workOrders.create(
                workOrderCommandId(commandId),
                correlationId,
                recommendation.assetId(),
                null,
                MaintenanceSourceType.TELEMETRY,
                recommendation.observationId(),
                MaintenanceClassification.CORRECTIVE,
                mapPriority(recommendation.severity()),
                recommendation.recommendedAction(),
                recommendation.evidence(),
                defaultChecklist(),
                actor
        );
        recommendation.attachWorkOrder(created.workOrder().id(), clock);
        recommendations.save(recommendation);
    }

    private static void requireExpectedVersion(MaintenanceRecommendation recommendation, long expectedVersion) {
        if (recommendation.version() != expectedVersion) {
            throw new StaleMaintenanceRecommendationVersionException(
                    recommendation.id(),
                    expectedVersion,
                    recommendation.version()
            );
        }
    }

    private RecommendationIngestResult create(
            String observationId,
            Instant observedAt,
            String assetCode,
            MaintenanceSignalType signalType,
            MaintenanceRecommendationSeverity severity,
            Double value,
            String unit,
            String evidence,
            String recommendedAction
    ) {
        MaintenanceAsset asset = assets.findByAssetCode(assetCode)
                .orElseThrow(() -> new MaintenanceAssetNotFoundException(assetCode));
        MaintenanceRecommendation recommendation = MaintenanceRecommendation.receive(
                MaintenanceRecommendationId.random(),
                observationId,
                observedAt,
                assetCode,
                asset.id(),
                signalType,
                severity,
                value,
                unit,
                evidence,
                recommendedAction,
                clock
        );
        recommendations.save(recommendation);
        return new RecommendationIngestResult(recommendation, false);
    }

    private static UUID workOrderCommandId(UUID recommendationCommandId) {
        return UUID.nameUUIDFromBytes(
                ("maintenance.recommendation.accept:" + recommendationCommandId).getBytes(StandardCharsets.UTF_8)
        );
    }

    private static MaintenancePriority mapPriority(MaintenanceRecommendationSeverity severity) {
        return switch (severity) {
            case CRITICAL -> MaintenancePriority.P1;
            case WARNING -> MaintenancePriority.P2;
            case INFO -> MaintenancePriority.P3;
        };
    }

    private static List<ChecklistItemDraft> defaultChecklist() {
        return List.of(
                new ChecklistItemDraft(1, "Inspect wheel assembly", null, true),
                new ChecklistItemDraft(2, "Verify sensor calibration", null, true)
        );
    }

    public record RecommendationIngestResult(MaintenanceRecommendation recommendation, boolean duplicate) {
    }
}
