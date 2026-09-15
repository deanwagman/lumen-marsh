package com.deanwagman.lumenmarsh.venueops.maintenance.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MaintenanceActivityJpaRepository extends JpaRepository<MaintenanceActivityEntity, String> {
    List<MaintenanceActivityEntity> findByWorkOrderIdOrderBySequenceAsc(String workOrderId);
}
