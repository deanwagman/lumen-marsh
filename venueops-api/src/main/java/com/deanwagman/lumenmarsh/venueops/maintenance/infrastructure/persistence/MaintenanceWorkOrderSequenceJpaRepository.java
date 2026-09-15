package com.deanwagman.lumenmarsh.venueops.maintenance.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MaintenanceWorkOrderSequenceJpaRepository extends JpaRepository<MaintenanceWorkOrderSequenceEntity, Integer> {

    @Query(value = """
            INSERT INTO maintenance_work_order_sequence (year, last_value)
            VALUES (:year, 1)
            ON CONFLICT (year) DO UPDATE
                SET last_value = maintenance_work_order_sequence.last_value + 1
            RETURNING last_value
            """, nativeQuery = true)
    long nextValue(@Param("year") int year);
}
