package com.deanwagman.lumenmarsh.venueops.attraction.infrastructure.persistence;

import com.deanwagman.lumenmarsh.venueops.attraction.domain.AttractionStatus;
import com.deanwagman.lumenmarsh.venueops.attraction.domain.CapacityMode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;

public interface AttractionJpaRepository extends JpaRepository<AttractionEntity, String> {

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update AttractionEntity a
               set a.status = :status,
                   a.capacityMode = :capacityMode,
                   a.waitMinutes = :waitMinutes,
                   a.updatedAt = :updatedAt,
                   a.version = :newVersion
             where a.id = :id
               and a.version = :expectedVersion
            """)
    int updateOperationalState(
            @Param("id") String id,
            @Param("status") AttractionStatus status,
            @Param("capacityMode") CapacityMode capacityMode,
            @Param("waitMinutes") Integer waitMinutes,
            @Param("updatedAt") Instant updatedAt,
            @Param("newVersion") long newVersion,
            @Param("expectedVersion") long expectedVersion
    );
}
