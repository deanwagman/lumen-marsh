package com.deanwagman.lumenmarsh.venueops.maintenance.infrastructure.persistence;

import com.deanwagman.lumenmarsh.venueops.maintenance.domain.workorder.MaintenanceWorkOrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface MaintenanceWorkOrderJpaRepository extends JpaRepository<MaintenanceWorkOrderEntity, String> {

    List<MaintenanceWorkOrderEntity> findByAssetId(String assetId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update MaintenanceWorkOrderEntity w
               set w.incidentId = :incidentId,
                   w.status = :status,
                   w.assignedTeam = :assignedTeam,
                   w.assignedActorSubject = :assignedActorSubject,
                   w.estimatedRestoreAt = :estimatedRestoreAt,
                   w.openedAt = :openedAt,
                   w.workStartedAt = :workStartedAt,
                   w.readyForTestingAt = :readyForTestingAt,
                   w.completedAt = :completedAt,
                   w.canceledAt = :canceledAt,
                   w.updatedAt = :updatedAt,
                   w.version = :newVersion
             where w.id = :id
               and w.version = :expectedVersion
            """)
    int updateOperationalState(
            @Param("id") String id,
            @Param("incidentId") String incidentId,
            @Param("status") MaintenanceWorkOrderStatus status,
            @Param("assignedTeam") String assignedTeam,
            @Param("assignedActorSubject") String assignedActorSubject,
            @Param("estimatedRestoreAt") Instant estimatedRestoreAt,
            @Param("openedAt") Instant openedAt,
            @Param("workStartedAt") Instant workStartedAt,
            @Param("readyForTestingAt") Instant readyForTestingAt,
            @Param("completedAt") Instant completedAt,
            @Param("canceledAt") Instant canceledAt,
            @Param("updatedAt") Instant updatedAt,
            @Param("newVersion") long newVersion,
            @Param("expectedVersion") long expectedVersion
    );
}
