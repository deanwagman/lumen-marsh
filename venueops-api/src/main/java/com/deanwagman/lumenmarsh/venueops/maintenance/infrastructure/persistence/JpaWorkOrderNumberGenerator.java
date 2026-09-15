package com.deanwagman.lumenmarsh.venueops.maintenance.infrastructure.persistence;

import com.deanwagman.lumenmarsh.venueops.maintenance.application.WorkOrderNumberGenerator;
import jakarta.persistence.EntityManager;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.ZoneOffset;

@Component
@ConditionalOnProperty(name = "venueops.attractions.persistence", havingValue = "jpa")
public class JpaWorkOrderNumberGenerator implements WorkOrderNumberGenerator {

    private final EntityManager entityManager;

    public JpaWorkOrderNumberGenerator(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    @Transactional
    public String next(Clock clock) {
        int year = clock.instant().atZone(ZoneOffset.UTC).getYear();
        Object result = entityManager.createNativeQuery("""
                        INSERT INTO maintenance_work_order_sequence (year, last_value)
                        VALUES (:year, 1)
                        ON CONFLICT (year) DO UPDATE
                            SET last_value = maintenance_work_order_sequence.last_value + 1
                        RETURNING last_value
                        """)
                .setParameter("year", year)
                .getSingleResult();
        long value = ((Number) result).longValue();
        return "LM-%d-%04d".formatted(year, value);
    }
}
