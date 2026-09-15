package com.deanwagman.lumenmarsh.venueops.maintenance.infrastructure.persistence;

import com.deanwagman.lumenmarsh.venueops.maintenance.application.MaintenanceRecommendationRepository;
import com.deanwagman.lumenmarsh.venueops.maintenance.application.StaleMaintenanceRecommendationVersionException;
import com.deanwagman.lumenmarsh.venueops.maintenance.domain.asset.MaintenanceAssetId;
import com.deanwagman.lumenmarsh.venueops.maintenance.domain.recommendation.MaintenanceRecommendation;
import com.deanwagman.lumenmarsh.venueops.maintenance.domain.recommendation.MaintenanceRecommendationId;
import com.deanwagman.lumenmarsh.venueops.maintenance.domain.recommendation.MaintenanceRecommendationSeverity;
import com.deanwagman.lumenmarsh.venueops.maintenance.domain.recommendation.MaintenanceRecommendationStatus;
import com.deanwagman.lumenmarsh.venueops.maintenance.domain.recommendation.MaintenanceSignalType;
import com.deanwagman.lumenmarsh.venueops.maintenance.domain.workorder.MaintenanceWorkOrderId;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Entity
@Table(name = "maintenance_recommendations")
class MaintenanceRecommendationEntity {

    @Id
    @Column(length = 36)
    private String id;

    @Column(name = "observation_id", nullable = false, unique = true, length = 128)
    private String observationId;

    @Column(name = "observed_at", nullable = false)
    private Instant observedAt;

    @Column(name = "asset_code", nullable = false, length = 64)
    private String assetCode;

    @Column(name = "asset_id", nullable = false, length = 36)
    private String assetId;

    @Enumerated(EnumType.STRING)
    @Column(name = "signal_type", nullable = false, length = 32)
    private MaintenanceSignalType signalType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private MaintenanceRecommendationSeverity severity;

    private Double value;
    private String unit;

    @Column(nullable = false, columnDefinition = "text")
    private String evidence;

    @Column(name = "recommended_action", nullable = false, columnDefinition = "text")
    private String recommendedAction;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private MaintenanceRecommendationStatus status;

    @Column(name = "work_order_id", length = 36)
    private String workOrderId;

    @Column(name = "command_id", length = 36)
    private String commandId;

    @Column(name = "received_at", nullable = false)
    private Instant receivedAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(nullable = false)
    private long version;

    protected MaintenanceRecommendationEntity() {
    }

    MaintenanceRecommendationEntity(
            String id,
            String observationId,
            Instant observedAt,
            String assetCode,
            String assetId,
            MaintenanceSignalType signalType,
            MaintenanceRecommendationSeverity severity,
            Double value,
            String unit,
            String evidence,
            String recommendedAction,
            MaintenanceRecommendationStatus status,
            String workOrderId,
            String commandId,
            Instant receivedAt,
            Instant updatedAt,
            long version
    ) {
        this.id = id;
        this.observationId = observationId;
        this.observedAt = observedAt;
        this.assetCode = assetCode;
        this.assetId = assetId;
        this.signalType = signalType;
        this.severity = severity;
        this.value = value;
        this.unit = unit;
        this.evidence = evidence;
        this.recommendedAction = recommendedAction;
        this.status = status;
        this.workOrderId = workOrderId;
        this.commandId = commandId;
        this.receivedAt = receivedAt;
        this.updatedAt = updatedAt;
        this.version = version;
    }

    String getId() {
        return id;
    }

    String getObservationId() {
        return observationId;
    }

    Instant getObservedAt() {
        return observedAt;
    }

    String getAssetCode() {
        return assetCode;
    }

    String getAssetId() {
        return assetId;
    }

    MaintenanceSignalType getSignalType() {
        return signalType;
    }

    MaintenanceRecommendationSeverity getSeverity() {
        return severity;
    }

    Double getValue() {
        return value;
    }

    String getUnit() {
        return unit;
    }

    String getEvidence() {
        return evidence;
    }

    String getRecommendedAction() {
        return recommendedAction;
    }

    MaintenanceRecommendationStatus getStatus() {
        return status;
    }

    String getWorkOrderId() {
        return workOrderId;
    }

    String getCommandId() {
        return commandId;
    }

    Instant getReceivedAt() {
        return receivedAt;
    }

    Instant getUpdatedAt() {
        return updatedAt;
    }

    long getVersion() {
        return version;
    }
}

interface MaintenanceRecommendationJpaRepository extends JpaRepository<MaintenanceRecommendationEntity, String> {
    Optional<MaintenanceRecommendationEntity> findByObservationId(String observationId);

    Optional<MaintenanceRecommendationEntity> findByCommandId(String commandId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update MaintenanceRecommendationEntity r
               set r.status = :status,
                   r.workOrderId = :workOrderId,
                   r.commandId = :commandId,
                   r.updatedAt = :updatedAt,
                   r.version = :newVersion
             where r.id = :id
               and r.version = :expectedVersion
            """)
    int updateOperationalState(
            @Param("id") String id,
            @Param("status") MaintenanceRecommendationStatus status,
            @Param("workOrderId") String workOrderId,
            @Param("commandId") String commandId,
            @Param("updatedAt") Instant updatedAt,
            @Param("newVersion") long newVersion,
            @Param("expectedVersion") long expectedVersion
    );
}

@Repository
@ConditionalOnProperty(name = "venueops.attractions.persistence", havingValue = "jpa")
public class JpaMaintenanceRecommendationRepository implements MaintenanceRecommendationRepository {

    private final MaintenanceRecommendationJpaRepository recommendations;

    public JpaMaintenanceRecommendationRepository(MaintenanceRecommendationJpaRepository recommendations) {
        this.recommendations = recommendations;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<MaintenanceRecommendation> findById(MaintenanceRecommendationId id) {
        return recommendations.findById(id.toString()).map(JpaMaintenanceRecommendationRepository::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<MaintenanceRecommendation> findByObservationId(String observationId) {
        return recommendations.findByObservationId(observationId).map(JpaMaintenanceRecommendationRepository::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<MaintenanceRecommendation> findByCommandId(UUID commandId) {
        return recommendations.findByCommandId(commandId.toString()).map(JpaMaintenanceRecommendationRepository::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MaintenanceRecommendation> findAll() {
        return recommendations.findAll().stream()
                .map(JpaMaintenanceRecommendationRepository::toDomain)
                .sorted(Comparator.comparing(MaintenanceRecommendation::observedAt).reversed())
                .toList();
    }

    @Override
    @Transactional
    public void save(MaintenanceRecommendation recommendation) {
        String id = recommendation.id().toString();
        Optional<MaintenanceRecommendationEntity> existing = recommendations.findById(id);
        if (existing.isEmpty()) {
            recommendations.save(toEntity(recommendation));
            recommendation.markVersionCommitted();
            return;
        }
        long expectedVersion = recommendation.version() - recommendation.uncommittedBumps();
        int updated = recommendations.updateOperationalState(
                id,
                recommendation.status(),
                recommendation.workOrderId() == null ? null : recommendation.workOrderId().toString(),
                recommendation.lastCommandId() == null ? null : recommendation.lastCommandId().toString(),
                recommendation.updatedAt(),
                recommendation.version(),
                expectedVersion
        );
        if (updated == 0) {
            throw new StaleMaintenanceRecommendationVersionException(
                    recommendation.id(),
                    expectedVersion,
                    existing.get().getVersion()
            );
        }
        recommendation.markVersionCommitted();
    }

    private static MaintenanceRecommendation toDomain(MaintenanceRecommendationEntity entity) {
        return new MaintenanceRecommendation(
                MaintenanceRecommendationId.of(entity.getId()),
                entity.getObservationId(),
                entity.getObservedAt(),
                entity.getAssetCode(),
                MaintenanceAssetId.of(entity.getAssetId()),
                entity.getSignalType(),
                entity.getSeverity(),
                entity.getValue(),
                entity.getUnit(),
                entity.getEvidence(),
                entity.getRecommendedAction(),
                entity.getStatus(),
                entity.getWorkOrderId() == null ? null : MaintenanceWorkOrderId.of(entity.getWorkOrderId()),
                entity.getReceivedAt(),
                entity.getUpdatedAt(),
                entity.getVersion(),
                entity.getCommandId() == null ? null : UUID.fromString(entity.getCommandId())
        );
    }

    private static MaintenanceRecommendationEntity toEntity(MaintenanceRecommendation recommendation) {
        return new MaintenanceRecommendationEntity(
                recommendation.id().toString(),
                recommendation.observationId(),
                recommendation.observedAt(),
                recommendation.assetCode(),
                recommendation.assetId().toString(),
                recommendation.signalType(),
                recommendation.severity(),
                recommendation.value(),
                recommendation.unit(),
                recommendation.evidence(),
                recommendation.recommendedAction(),
                recommendation.status(),
                recommendation.workOrderId() == null ? null : recommendation.workOrderId().toString(),
                recommendation.lastCommandId() == null ? null : recommendation.lastCommandId().toString(),
                recommendation.receivedAt(),
                recommendation.updatedAt(),
                recommendation.version()
        );
    }
}
