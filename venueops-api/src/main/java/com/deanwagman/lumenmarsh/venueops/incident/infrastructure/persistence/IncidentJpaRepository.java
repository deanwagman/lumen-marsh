package com.deanwagman.lumenmarsh.venueops.incident.infrastructure.persistence;

import com.deanwagman.lumenmarsh.venueops.incident.domain.IncidentSeverity;
import com.deanwagman.lumenmarsh.venueops.incident.domain.IncidentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;

public interface IncidentJpaRepository extends JpaRepository<IncidentEntity, String> {

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update IncidentEntity i
               set i.severity = :severity,
                   i.status = :status,
                   i.assignedTo = :assignedTo,
                   i.guestTitle = :guestTitle,
                   i.guestMessage = :guestMessage,
                   i.guestAdvisoryPublished = :guestAdvisoryPublished,
                   i.updatedAt = :updatedAt,
                   i.version = :newVersion
             where i.id = :id
               and i.version = :expectedVersion
            """)
    int updateOperationalState(
            @Param("id") String id,
            @Param("severity") IncidentSeverity severity,
            @Param("status") IncidentStatus status,
            @Param("assignedTo") String assignedTo,
            @Param("guestTitle") String guestTitle,
            @Param("guestMessage") String guestMessage,
            @Param("guestAdvisoryPublished") boolean guestAdvisoryPublished,
            @Param("updatedAt") Instant updatedAt,
            @Param("newVersion") long newVersion,
            @Param("expectedVersion") long expectedVersion
    );
}
