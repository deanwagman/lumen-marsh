package com.deanwagman.lumenmarsh.venueops.maintenance.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MaintenanceChecklistItemJpaRepository extends JpaRepository<MaintenanceChecklistItemEntity, String> {
    List<MaintenanceChecklistItemEntity> findByWorkOrderIdOrderBySequenceAsc(String workOrderId);
}
